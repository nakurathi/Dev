"""
api/main.py
-----------
FastAPI application entry point.
Registers all routers, middleware, and exception handlers.
"""
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from api.routes import patients, appointments, analytics

app = FastAPI(
    title="Healthcare Analytics API",
    description="Python showcase — FastAPI + pandas + scikit-learn + Kafka + GCP",
    version="1.0.0",
)

# ── CORS ──────────────────────────────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://localhost:3000"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Routers ───────────────────────────────────────────────────────────────────
app.include_router(patients.router,     prefix="/api/patients",     tags=["Patients"])
app.include_router(appointments.router, prefix="/api/appointments", tags=["Appointments"])
app.include_router(analytics.router,    prefix="/api/analytics",    tags=["Analytics"])


# ── Global exception handler ──────────────────────────────────────────────────
@app.exception_handler(ValueError)
async def value_error_handler(request: Request, exc: ValueError) -> JSONResponse:
    return JSONResponse(status_code=400, content={"success": False, "message": str(exc)})


@app.exception_handler(KeyError)
async def key_error_handler(request: Request, exc: KeyError) -> JSONResponse:
    return JSONResponse(status_code=404, content={"success": False, "message": f"Not found: {exc}"})


# ── Health check ──────────────────────────────────────────────────────────────
@app.get("/health", tags=["Health"])
async def health():
    return {"status": "UP", "service": "healthcare-analytics-python"}
