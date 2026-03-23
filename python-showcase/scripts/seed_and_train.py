"""
scripts/seed_and_train.py
--------------------------
CLI script to seed data and train the ML model.
Run with: python scripts/seed_and_train.py

Demonstrates:
- argparse CLI
- Running pandas analytics
- Training the ML model
- Printing formatted results
"""
import argparse
import json
import sys
import os

# Add parent dir to path so imports work
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from data.patient_analytics import PatientAnalytics
from ml.readmission_model import ReadmissionModel


def run_analytics():
    print("\n── Patient Analytics ─────────────────────────────────")
    analytics = PatientAnalytics()

    stats = analytics.get_summary_stats()
    print(f"  Total patients:    {stats['total_patients']}")
    print(f"  Active patients:   {stats['active_patients']}")
    print(f"  High risk:         {stats['high_risk_patients']}")
    print(f"  Average age:       {stats['average_age']}")
    print(f"  Diabetic %:        {stats['diabetic_percent']}%")

    print("\n  Risk Distribution:")
    dist = analytics.get_risk_distribution()
    for level, count in dist.items():
        print(f"    {level:8s}: {count}")

    print("\n  Top Diagnoses:")
    for dx in analytics.get_top_diagnoses(top_n=3):
        print(f"    {dx['diagnosis_code']} — {dx['count']} cases, "
              f"readmission rate: {dx['readmission_rate']}%")


def run_training():
    print("\n── ML Model Training ─────────────────────────────────")
    model   = ReadmissionModel()
    metrics = model.train()

    print(f"  Model version: {metrics['model_version']}")
    print(f"  Accuracy:      {metrics['accuracy'] * 100:.1f}%")
    print(f"  Training set:  {metrics['training_samples']} samples")
    print(f"  Test set:      {metrics['test_samples']} samples")

    print("\n  Feature Importances:")
    for feature, importance in list(metrics["feature_importances"].items())[:5]:
        bar = "█" * int(importance * 50)
        print(f"    {feature:30s} {bar} {importance:.4f}")


def main():
    parser = argparse.ArgumentParser(description="Healthcare Analytics — Seed & Train")
    parser.add_argument("--analytics", action="store_true", help="Run patient analytics")
    parser.add_argument("--train",     action="store_true", help="Train the ML model")
    parser.add_argument("--all",       action="store_true", help="Run everything")
    args = parser.parse_args()

    if args.all or args.analytics:
        run_analytics()

    if args.all or args.train:
        run_training()

    if not any(vars(args).values()):
        parser.print_help()

    print("\nDone! ✓")


if __name__ == "__main__":
    main()
