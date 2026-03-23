"""
tests/test_api.py
-----------------
FastAPI endpoint tests using httpx TestClient.
Demonstrates: fixtures, parametrize, async tests, status code assertions.
"""
import pytest
from datetime import datetime, timedelta
from fastapi.testclient import TestClient
from api.main import app

client = TestClient(app)


# ── Health check ──────────────────────────────────────────────────────────────

def test_health_check():
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "UP"


# ── Patient tests ─────────────────────────────────────────────────────────────

@pytest.fixture
def valid_patient():
    return {
        "first_name":    "Jane",
        "last_name":     "Doe",
        "email":         "jane.doe@example.com",
        "date_of_birth": "1990-05-15",
        "status":        "ACTIVE",
    }


def test_create_patient_success(valid_patient):
    resp = client.post("/api/patients", json=valid_patient)
    assert resp.status_code == 201
    body = resp.json()
    assert body["success"] is True
    assert body["data"]["email"] == valid_patient["email"]
    assert "patient_id" in body["data"]


def test_create_patient_invalid_email():
    resp = client.post("/api/patients", json={
        "first_name": "Bad", "last_name": "Email",
        "email": "not-an-email", "date_of_birth": "1990-01-01",
    })
    assert resp.status_code == 422   # Pydantic validation error


def test_create_patient_future_dob():
    resp = client.post("/api/patients", json={
        "first_name": "Future", "last_name": "Person",
        "email": "future@test.com",
        "date_of_birth": (datetime.today() + timedelta(days=1)).strftime("%Y-%m-%d"),
    })
    assert resp.status_code == 422


def test_get_patient_not_found():
    resp = client.get("/api/patients/nonexistent-id")
    assert resp.status_code == 404


def test_create_and_get_patient(valid_patient):
    # Use unique email to avoid conflict with other tests
    valid_patient["email"] = "get.test@example.com"
    create_resp = client.post("/api/patients", json=valid_patient)
    patient_id  = create_resp.json()["data"]["patient_id"]

    get_resp = client.get(f"/api/patients/{patient_id}")
    assert get_resp.status_code == 200
    assert get_resp.json()["data"]["patient_id"] == patient_id


@pytest.mark.parametrize("status", ["ACTIVE", "INACTIVE", "ARCHIVED"])
def test_list_patients_by_status(status):
    resp = client.get(f"/api/patients?status={status}")
    assert resp.status_code == 200
    body = resp.json()
    assert body["success"] is True
    assert isinstance(body["data"], list)


# ── Appointment tests ─────────────────────────────────────────────────────────

@pytest.fixture
def valid_appointment():
    future_time = (datetime.utcnow() + timedelta(days=3)).isoformat()
    return {
        "patient_id":   "patient-001",
        "doctor_id":    "doctor-001",
        "scheduled_at": future_time,
        "notes":        "Annual checkup",
    }


def test_schedule_appointment_success(valid_appointment):
    resp = client.post("/api/appointments", json=valid_appointment)
    assert resp.status_code == 201
    body = resp.json()
    assert body["success"] is True
    assert body["data"]["status"] == "SCHEDULED"


def test_schedule_appointment_past_date():
    past_time = (datetime.utcnow() - timedelta(days=1)).isoformat()
    resp = client.post("/api/appointments", json={
        "patient_id": "p-001", "doctor_id": "d-001",
        "scheduled_at": past_time,
    })
    assert resp.status_code == 422


def test_appointment_status_transition(valid_appointment):
    # Create appointment
    appt_resp = client.post("/api/appointments", json=valid_appointment)
    appt_id   = appt_resp.json()["data"]["appointment_id"]

    # Confirm it
    confirm_resp = client.patch(f"/api/appointments/{appt_id}/status?new_status=CONFIRMED")
    assert confirm_resp.status_code == 200
    assert confirm_resp.json()["data"]["status"] == "CONFIRMED"


def test_invalid_status_transition(valid_appointment):
    # Create + complete the appointment
    appt_resp = client.post("/api/appointments", json=valid_appointment)
    appt_id   = appt_resp.json()["data"]["appointment_id"]
    client.patch(f"/api/appointments/{appt_id}/status?new_status=CONFIRMED")
    client.patch(f"/api/appointments/{appt_id}/status?new_status=COMPLETED")

    # Try to go back to SCHEDULED — should fail
    bad_resp = client.patch(f"/api/appointments/{appt_id}/status?new_status=SCHEDULED")
    assert bad_resp.status_code == 400
