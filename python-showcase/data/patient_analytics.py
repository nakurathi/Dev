"""
data/patient_analytics.py
--------------------------
Demonstrates intermediate pandas skills:
- DataFrame creation and manipulation
- groupby, agg, pivot_table
- datetime operations
- Data cleaning (fillna, dropna, type casting)
- Merging DataFrames
- Rolling averages and trends
"""
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import Any


class PatientAnalytics:
    """
    Analytics engine using pandas for patient data processing.
    In production this would read from BigQuery or PostgreSQL.
    Here it uses synthetic data to showcase pandas operations.
    """

    def __init__(self):
        # Generate synthetic data once on init
        self._patients_df    = self._generate_patients()
        self._admissions_df  = self._generate_admissions()

    # ── Data Generation ───────────────────────────────────────────────────────

    def _generate_patients(self) -> pd.DataFrame:
        """Generate synthetic patient DataFrame."""
        np.random.seed(42)
        n = 500

        df = pd.DataFrame({
            "patient_id":   [f"P{i:04d}" for i in range(1, n + 1)],
            "age":          np.random.randint(18, 90, n),
            "gender":       np.random.choice(["M", "F", "Other"], n, p=[0.48, 0.48, 0.04]),
            "status":       np.random.choice(["ACTIVE", "INACTIVE", "ARCHIVED"], n, p=[0.75, 0.15, 0.10]),
            "risk_level":   np.random.choice(["LOW", "MEDIUM", "HIGH"], n, p=[0.60, 0.30, 0.10]),
            "num_visits":   np.random.poisson(3, n),
            "has_diabetes": np.random.choice([True, False], n, p=[0.15, 0.85]),
            "state":        np.random.choice(["MI", "OH", "IL", "NY", "CA"], n),
        })

        # Introduce some nulls to show cleaning
        null_idx = np.random.choice(df.index, size=20, replace=False)
        df.loc[null_idx, "risk_level"] = None

        return df

    def _generate_admissions(self) -> pd.DataFrame:
        """Generate synthetic hospital admissions with datetime index."""
        np.random.seed(99)
        n = 1000
        today = datetime.today()

        df = pd.DataFrame({
            "admission_id":  [f"ADM{i:05d}" for i in range(1, n + 1)],
            "patient_id":    [f"P{np.random.randint(1, 501):04d}" for _ in range(n)],
            "admitted_at":   [today - timedelta(days=int(d)) for d in np.random.randint(0, 365, n)],
            "length_of_stay": np.random.randint(1, 21, n),
            "diagnosis_code": np.random.choice(["E11", "I10", "J18", "K92", "M79"], n),
            "readmitted":    np.random.choice([True, False], n, p=[0.18, 0.82]),
            "cost":          np.round(np.random.uniform(1500, 45000, n), 2),
        })
        return df

    # ── Analytics Methods ─────────────────────────────────────────────────────

    def get_summary_stats(self) -> dict[str, Any]:
        """
        Build dashboard stats using pandas aggregations.
        Shows: value_counts, mean, conditional filtering, fillna.
        """
        df = self._patients_df.copy()

        # Fill nulls before aggregating
        df["risk_level"] = df["risk_level"].fillna("LOW")

        total          = len(df)
        active         = (df["status"] == "ACTIVE").sum()
        high_risk      = (df["risk_level"] == "HIGH").sum()
        avg_age        = round(df["age"].mean(), 1)
        diabetic_pct   = round((df["has_diabetes"].sum() / total) * 100, 1)

        # Age group bucketing with pd.cut
        df["age_group"] = pd.cut(
            df["age"],
            bins=[0, 30, 50, 65, 120],
            labels=["18-30", "31-50", "51-65", "65+"],
        )
        age_distribution = df["age_group"].value_counts().to_dict()
        age_distribution = {str(k): int(v) for k, v in age_distribution.items()}

        return {
            "total_patients":    int(total),
            "active_patients":   int(active),
            "high_risk_patients": int(high_risk),
            "average_age":       avg_age,
            "diabetic_percent":  diabetic_pct,
            "age_distribution":  age_distribution,
        }

    def get_risk_distribution(self) -> dict[str, int]:
        """
        Group patients by risk level.
        Shows: groupby + value_counts.
        """
        df = self._patients_df.copy()
        df["risk_level"] = df["risk_level"].fillna("LOW")

        dist = df["risk_level"].value_counts().to_dict()
        return {k: int(v) for k, v in dist.items()}

    def get_admission_trends(self, days: int = 30) -> list[dict]:
        """
        Daily admission counts over the last N days.
        Shows: datetime filtering, resample, rolling average.
        """
        df = self._admissions_df.copy()

        # Filter to last N days
        cutoff = datetime.today() - timedelta(days=days)
        df     = df[df["admitted_at"] >= cutoff].copy()

        # Set datetime index and resample by day
        df.set_index("admitted_at", inplace=True)
        daily = df.resample("D")["admission_id"].count().reset_index()
        daily.columns = ["date", "admissions"]

        # 7-day rolling average
        daily["rolling_avg"] = daily["admissions"].rolling(window=7, min_periods=1).mean().round(1)

        return daily.to_dict(orient="records")

    def get_top_diagnoses(self, top_n: int = 5) -> list[dict]:
        """
        Top N most common diagnosis codes with avg cost.
        Shows: groupby + agg + sort_values.
        """
        result = (
            self._admissions_df
            .groupby("diagnosis_code")
            .agg(
                count=("admission_id", "count"),
                avg_cost=("cost", "mean"),
                avg_los=("length_of_stay", "mean"),
                readmission_rate=("readmitted", "mean"),
            )
            .sort_values("count", ascending=False)
            .head(top_n)
            .reset_index()
        )

        result["avg_cost"]          = result["avg_cost"].round(2)
        result["avg_los"]           = result["avg_los"].round(1)
        result["readmission_rate"]  = (result["readmission_rate"] * 100).round(1)

        return result.to_dict(orient="records")

    def merge_patient_admissions(self) -> pd.DataFrame:
        """
        Merge patients and admissions DataFrames.
        Shows: pd.merge (left join), handling missing join keys.
        """
        merged = pd.merge(
            self._patients_df,
            self._admissions_df,
            on="patient_id",
            how="left",
        )
        return merged

    def get_high_cost_patients(self, threshold: float = 20000.0) -> pd.DataFrame:
        """
        Find patients with total admission cost above threshold.
        Shows: groupby sum, merge, boolean masking.
        """
        cost_by_patient = (
            self._admissions_df
            .groupby("patient_id")["cost"]
            .sum()
            .reset_index()
            .rename(columns={"cost": "total_cost"})
        )

        high_cost = cost_by_patient[cost_by_patient["total_cost"] >= threshold]

        return pd.merge(high_cost, self._patients_df[["patient_id", "age", "status"]], on="patient_id")
