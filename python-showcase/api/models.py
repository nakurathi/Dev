"""
api/models.py
-------------
Pydantic v2 models for API request validation and response serialization.
Demonstrates field validation, custom validators, and generic response wrapper.
"""
from __future__ import annotations
from datetime import date, datetime
from enum import Enum
from typing import Generic, TypeVar
from pydantic import BaseModel, EmailStr, Field, field_validator, ConfigDict

T = TypeVar("T")


# ── Enums ─────────────────────────────────────────────────────────────────────

class PatientStatus(str, Enum):
    ACTIVE   = "ACTIVE"
    INACTIVE = "INACTIVE"
    ARCHIVED = "ARCHIVED"


class AppointmentStatus(str, Enum):
    SCHEDULED = "SCHEDULED"
    CONFIRMED = "CONFIRMED"
    CANCELLED = "CANCELLED"
    COMPLETED = "COMPLETED"


class RiskLevel(str, Enum):
    LOW    = "LOW"
    MEDIUM = "MEDIUM"
    HIGH   = "HIGH"


# ── Generic response wrapper ──────────────────────────────────────────────────

class ApiResponse(BaseModel, Generic[T]):
    """Standard JSON envelope for all endpoints."""
    success: bool
    message: str
    data: T | None = None

    @classmethod
    def ok(cls, data: T, message: str = "OK") -> "ApiResponse[T]":
        return cls(success=True, message=message, data=data)

    @classmethod
    def error(cls, message: str) -> "ApiResponse[None]":
        return cls(success=False, message=message)


# ── Patient ───────────────────────────────────────────────────────────────────

class PatientCreate(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    first_name:    str      = Field(..., min_length=1, max_length=100)
    last_name:     str      = Field(..., min_length=1, max_length=100)
    email:         EmailStr
    date_of_birth: date
    status:        PatientStatus = PatientStatus.ACTIVE

    @field_validator("date_of_birth")
    @classmethod
    def dob_must_be_past(cls, v: date) -> date:
        if v >= date.today():
            raise ValueError("Date of birth must be in the past")
        return v


class PatientResponse(BaseModel):
    patient_id:    str
    first_name:    str
    last_name:     str
    email:         str
    date_of_birth: date
    status:        PatientStatus
    created_at:    datetime


# ── Appointment ───────────────────────────────────────────────────────────────

class AppointmentCreate(BaseModel):
    patient_id:   str      = Field(..., min_length=1)
    doctor_id:    str      = Field(..., min_length=1)
    scheduled_at: datetime
    notes:        str | None = None

    @field_validator("scheduled_at")
    @classmethod
    def must_be_future(cls, v: datetime) -> datetime:
        if v.replace(tzinfo=None) <= datetime.now():
            raise ValueError("scheduled_at must be in the future")
        return v


class AppointmentResponse(BaseModel):
    appointment_id: str
    patient_id:     str
    doctor_id:      str
    scheduled_at:   datetime
    status:         AppointmentStatus
    notes:          str | None
    created_at:     datetime


# ── ML Prediction ─────────────────────────────────────────────────────────────

class ReadmissionFeatures(BaseModel):
    """Input features for 30-day hospital readmission risk model."""
    age:                   int   = Field(..., ge=0,  le=120)
    num_prior_admissions:  int   = Field(..., ge=0)
    num_medications:       int   = Field(..., ge=0)
    num_diagnoses:         int   = Field(..., ge=1)
    length_of_stay_days:   int   = Field(..., ge=1)
    has_diabetes:          bool  = False
    has_hypertension:      bool  = False
    has_heart_disease:     bool  = False


class ReadmissionPrediction(BaseModel):
    patient_id:       str
    risk_score:       float = Field(..., ge=0.0, le=1.0)
    risk_level:       RiskLevel
    top_risk_factors: list[str]
    predicted_at:     datetime
