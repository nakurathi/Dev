"""
ml/readmission_model.py
------------------------
30-day hospital readmission risk prediction using scikit-learn.

Demonstrates:
- RandomForestClassifier training pipeline
- Feature engineering with pandas
- Model persistence with joblib
- Prediction with risk level classification
- Feature importance extraction
"""
import os
import uuid
import joblib
import numpy as np
import pandas as pd
from datetime import datetime
from pathlib import Path

from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.metrics import accuracy_score, classification_report

from api.models import ReadmissionFeatures, ReadmissionPrediction, RiskLevel


FEATURE_COLUMNS = [
    "age",
    "num_prior_admissions",
    "num_medications",
    "num_diagnoses",
    "length_of_stay_days",
    "has_diabetes",
    "has_hypertension",
    "has_heart_disease",
]

MODEL_PATH = Path("models/readmission_model.joblib")


class ReadmissionModel:
    """
    RandomForest-based 30-day readmission risk classifier.

    Usage:
        model = ReadmissionModel()
        metrics = model.train()          # train on synthetic data
        pred = model.predict("P001", features)
    """

    def __init__(self):
        self._pipeline: Pipeline | None = None
        self._model_version: str = "v0.0"
        self._load_if_exists()

    # ── Properties ─────────────────────────────────────────────────────────────

    @property
    def is_trained(self) -> bool:
        return self._pipeline is not None

    # ── Training ───────────────────────────────────────────────────────────────

    def train(self) -> dict:
        """
        Train the readmission model on synthetic data.

        Returns dict with accuracy, classification report, and feature importances.
        """
        X, y = self._generate_training_data(n_samples=2000)

        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42, stratify=y
        )

        # Build sklearn Pipeline: scaler + classifier
        self._pipeline = Pipeline([
            ("scaler",     StandardScaler()),
            ("classifier", RandomForestClassifier(
                n_estimators=100,
                max_depth=6,
                min_samples_leaf=10,
                class_weight="balanced",  # handles class imbalance
                random_state=42,
                n_jobs=-1,               # use all CPU cores
            )),
        ])

        self._pipeline.fit(X_train, y_train)

        # Evaluate
        y_pred          = self._pipeline.predict(X_test)
        accuracy        = accuracy_score(y_test, y_pred)
        self._model_version = f"v1.{datetime.now().strftime('%Y%m%d')}"

        # Save model to disk
        MODEL_PATH.parent.mkdir(exist_ok=True)
        joblib.dump({"pipeline": self._pipeline, "version": self._model_version}, MODEL_PATH)

        # Feature importances from the RandomForest
        rf         = self._pipeline.named_steps["classifier"]
        importances = dict(zip(FEATURE_COLUMNS, rf.feature_importances_.round(4)))
        sorted_imp  = dict(sorted(importances.items(), key=lambda x: x[1], reverse=True))

        return {
            "model_version":       self._model_version,
            "accuracy":            round(accuracy, 4),
            "training_samples":    len(X_train),
            "test_samples":        len(X_test),
            "feature_importances": sorted_imp,
        }

    # ── Prediction ─────────────────────────────────────────────────────────────

    def predict(self, patient_id: str, features: ReadmissionFeatures) -> ReadmissionPrediction:
        """
        Predict readmission risk for a single patient.

        Returns ReadmissionPrediction with risk score, level, and top factors.
        """
        if not self.is_trained:
            raise RuntimeError("Model not trained. Call train() first.")

        # Build feature vector as DataFrame (preserves column names)
        X = pd.DataFrame([{
            "age":                   features.age,
            "num_prior_admissions":  features.num_prior_admissions,
            "num_medications":       features.num_medications,
            "num_diagnoses":         features.num_diagnoses,
            "length_of_stay_days":   features.length_of_stay_days,
            "has_diabetes":          int(features.has_diabetes),
            "has_hypertension":      int(features.has_hypertension),
            "has_heart_disease":     int(features.has_heart_disease),
        }])

        # Predict probability of readmission (class 1)
        proba      = self._pipeline.predict_proba(X)[0][1]
        risk_level = self._classify_risk(proba)

        # Top contributing features (based on feature importance)
        rf          = self._pipeline.named_steps["classifier"]
        importances = rf.feature_importances_
        top_indices = np.argsort(importances)[::-1][:3]
        top_factors = [FEATURE_COLUMNS[i] for i in top_indices]

        return ReadmissionPrediction(
            patient_id       = patient_id,
            risk_score       = round(float(proba), 4),
            risk_level       = risk_level,
            top_risk_factors = top_factors,
            predicted_at     = datetime.utcnow(),
        )

    # ── Helpers ────────────────────────────────────────────────────────────────

    def _classify_risk(self, score: float) -> RiskLevel:
        if score >= 0.6:
            return RiskLevel.HIGH
        elif score >= 0.35:
            return RiskLevel.MEDIUM
        return RiskLevel.LOW

    def _load_if_exists(self) -> None:
        """Load a previously saved model from disk if it exists."""
        if MODEL_PATH.exists():
            saved = joblib.load(MODEL_PATH)
            self._pipeline      = saved["pipeline"]
            self._model_version = saved["version"]

    def _generate_training_data(self, n_samples: int = 2000):
        """
        Generate synthetic training data with realistic feature correlations.
        Older patients with more diagnoses and prior admissions are more likely to be readmitted.
        """
        np.random.seed(42)
        n = n_samples

        age                  = np.random.randint(18, 90, n)
        num_prior_admissions = np.random.poisson(1.5, n)
        num_medications      = np.random.randint(0, 20, n)
        num_diagnoses        = np.random.randint(1, 10, n)
        length_of_stay_days  = np.random.randint(1, 21, n)
        has_diabetes         = np.random.binomial(1, 0.15, n)
        has_hypertension     = np.random.binomial(1, 0.25, n)
        has_heart_disease    = np.random.binomial(1, 0.10, n)

        # Readmission probability driven by features
        readmission_prob = (
            0.05
            + 0.004 * age
            + 0.08  * num_prior_admissions
            + 0.01  * num_medications
            + 0.02  * num_diagnoses
            + 0.005 * length_of_stay_days
            + 0.10  * has_diabetes
            + 0.08  * has_hypertension
            + 0.12  * has_heart_disease
        )
        readmission_prob = np.clip(readmission_prob, 0, 1)
        y = np.random.binomial(1, readmission_prob)

        X = pd.DataFrame({
            "age":                   age,
            "num_prior_admissions":  num_prior_admissions,
            "num_medications":       num_medications,
            "num_diagnoses":         num_diagnoses,
            "length_of_stay_days":   length_of_stay_days,
            "has_diabetes":          has_diabetes,
            "has_hypertension":      has_hypertension,
            "has_heart_disease":     has_heart_disease,
        })
        return X, y
