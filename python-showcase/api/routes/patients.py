"""
api/routes/patients.py
-----------------------
Patient CRUD endpoints using FastAPI.
Demonstrates: path params, query params, request body validation,
in-memory store (swap with DB in production), dependency injection.
"""
import uuid
from datetime import datetime
from typing import Optional
from fastapi import APIRouter, HTTPException, Query, status

from api.models import (
    PatientCreate, PatientResponse, ApiResponse, PatientStatus
)

router = APIRouter()

# ── In-memory store (replace with SQLAlchemy + PostgreSQL in production) ──────
_patients: dict[str, dict] = {}


@router.post("", response_model=ApiResponse[PatientResponse], status_code=201)
async def create_patient(payload: PatientCreate):
    """
    Create a new patient record.
    Validates email format, date of birth (must be past), and required fields.
    """
    # Check for duplicate email
    if any(p["email"] == payload.email for p in _patients.values()):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Patient with email {payload.email} already exists",
        )

    patient_id = str(uuid.uuid4())
    record = {
        "patient_id":    patient_id,
        "first_name":    payload.first_name,
        "last_name":     payload.last_name,
        "email":         str(payload.email),
        "date_of_birth": payload.date_of_birth,
        "status":        payload.status,
        "created_at":    datetime.utcnow(),
    }
    _patients[patient_id] = record
    return ApiResponse.ok(PatientResponse(**record), "Patient created successfully")


@router.get("/{patient_id}", response_model=ApiResponse[PatientResponse])
async def get_patient(patient_id: str):
    """Get a single patient by ID."""
    patient = _patients.get(patient_id)
    if not patient:
        raise HTTPException(status_code=404, detail=f"Patient {patient_id} not found")
    return ApiResponse.ok(PatientResponse(**patient))


@router.get("", response_model=ApiResponse[list[PatientResponse]])
async def list_patients(
    status: Optional[PatientStatus] = Query(default=None),
    limit:  int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0,  ge=0),
):
    """
    List patients with optional status filter and pagination.
    Demonstrates Query params with validation.
    """
    patients = list(_patients.values())
    if status:
        patients = [p for p in patients if p["status"] == status]

    total    = len(patients)
    patients = patients[offset : offset + limit]

    return ApiResponse.ok(
        [PatientResponse(**p) for p in patients],
        f"Found {total} patients",
    )


@router.patch("/{patient_id}/status", response_model=ApiResponse[PatientResponse])
async def update_status(patient_id: str, new_status: PatientStatus):
    """Update patient status."""
    if patient_id not in _patients:
        raise HTTPException(status_code=404, detail=f"Patient {patient_id} not found")

    _patients[patient_id]["status"] = new_status
    return ApiResponse.ok(
        PatientResponse(**_patients[patient_id]),
        f"Status updated to {new_status}",
    )


@router.delete("/{patient_id}", status_code=204)
async def delete_patient(patient_id: str):
    """Soft-delete by archiving."""
    if patient_id not in _patients:
        raise HTTPException(status_code=404, detail=f"Patient {patient_id} not found")
    _patients[patient_id]["status"] = PatientStatus.ARCHIVED
