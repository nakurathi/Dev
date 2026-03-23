"""
tests/test_analytics.py
------------------------
Unit tests for PatientAnalytics pandas operations.
"""
import pytest
from data.patient_analytics import PatientAnalytics


@pytest.fixture(scope="module")
def analytics():
    return PatientAnalytics()


def test_summary_stats_keys(analytics):
    stats = analytics.get_summary_stats()
    assert "total_patients"    in stats
    assert "active_patients"   in stats
    assert "high_risk_patients" in stats
    assert "average_age"       in stats
    assert "age_distribution"  in stats


def test_summary_stats_values(analytics):
    stats = analytics.get_summary_stats()
    assert stats["total_patients"]  == 500
    assert stats["active_patients"] <= stats["total_patients"]
    assert 0 < stats["average_age"] < 120


def test_risk_distribution_keys(analytics):
    dist = analytics.get_risk_distribution()
    assert "LOW"    in dist
    assert "MEDIUM" in dist
    assert "HIGH"   in dist


def test_risk_distribution_sums_to_total(analytics):
    dist  = analytics.get_risk_distribution()
    total = sum(dist.values())
    assert total == 500


def test_admission_trends_returns_list(analytics):
    trends = analytics.get_admission_trends(days=30)
    assert isinstance(trends, list)
    assert len(trends) > 0


def test_admission_trends_has_rolling_avg(analytics):
    trends = analytics.get_admission_trends(days=30)
    assert "rolling_avg" in trends[0]
    assert "admissions"  in trends[0]


def test_top_diagnoses(analytics):
    results = analytics.get_top_diagnoses(top_n=3)
    assert len(results) == 3
    assert "diagnosis_code"     in results[0]
    assert "count"              in results[0]
    assert "readmission_rate"   in results[0]


def test_high_cost_patients(analytics):
    df = analytics.get_high_cost_patients(threshold=20000.0)
    assert "total_cost" in df.columns
    assert (df["total_cost"] >= 20000.0).all()
