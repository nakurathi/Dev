"""
api/routes/appointments.py
---------------------------
Appointment scheduling endpoints.
Demonstrates: nested validation, status machine logic,
filtering by patient, async endpoint pattern.
"""
import uuid
from datetime import datetime
from typing import Optional
from fastapi import APIRouter, HTTPException, Query

from api.models import (
    AppointmentCreate, AppointmentResponse,
    ApiResponse, AppointmentStatus
)

router = APIRouter()

# ── In-memory store ───────────────────────────────────────────────────────────
_appointments: dict[str, dict] = {}

# Valid status transitions
_VALID_TRANSITIONS: dict[AppointmentStatus, list[AppointmentStatus]] = {
    AppointmentStatus.SCHEDULED:  [AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED],
    AppointmentStatus.CONFIRMED:  [AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED],
    AppointmentStatus.CANCELLED:  [],
    AppointmentStatus.COMPLETED:  [],
}


@router.post("", response_model=ApiResponse[AppointmentResponse], status_code=201)
async def schedule_appointment(payload: AppointmentCreate):
    """Schedule a new appointment. Validates that scheduledAt is in the future."""
    appt_id = str(uuid.uuid4())
    record = {
        "appointment_id": appt_id,
        "patient_id":     payload.patient_id,
        "doctor_id":      payload.doctor_id,
        "scheduled_at":   payload.scheduled_at,
        "status":         AppointmentStatus.SCHEDULED,
        "notes":          payload.notes,
        "created_at":     datetime.utcnow(),
    }
    _appointments[appt_id] = record
    return ApiResponse.ok(
        AppointmentResponse(**record),
        "Appointment scheduled successfully",
    )


@router.get("/{appointment_id}", response_model=ApiResponse[AppointmentResponse])
async def get_appointment(appointment_id: str):
    appt = _appointments.get(appointment_id)
    if not appt:
        raise HTTPException(status_code=404, detail=f"Appointment {appointment_id} not found")
    return ApiResponse.ok(AppointmentResponse(**appt))


@router.get("", response_model=ApiResponse[list[AppointmentResponse]])
async def list_appointments(
    patient_id: Optional[str]              = Query(default=None),
    status:     Optional[AppointmentStatus] = Query(default=None),
):
    """List appointments filtered by patient_id and/or status."""
    appts = list(_appointments.values())
    if patient_id:
        appts = [a for a in appts if a["patient_id"] == patient_id]
    if status:
        appts = [a for a in appts if a["status"] == status]
    return ApiResponse.ok([AppointmentResponse(**a) for a in appts])


@router.patch("/{appointment_id}/status", response_model=ApiResponse[AppointmentResponse])
async def update_status(appointment_id: str, new_status: AppointmentStatus):
    """
    Update appointment status using a state machine.
    Only valid transitions are allowed — e.g. COMPLETED → SCHEDULED is rejected.
    """
    if appointment_id not in _appointments:
        raise HTTPException(status_code=404, detail=f"Appointment {appointment_id} not found")

    appt           = _appointments[appointment_id]
    current_status = appt["status"]
    allowed        = _VALID_TRANSITIONS.get(current_status, [])

    if new_status not in allowed:
        raise HTTPException(
            status_code=400,
            detail=f"Cannot transition from {current_status} to {new_status}. "
                   f"Allowed: {[s.value for s in allowed]}",
        )

    appt["status"] = new_status
    return ApiResponse.ok(
        AppointmentResponse(**appt),
        f"Status updated to {new_status}",
    )
