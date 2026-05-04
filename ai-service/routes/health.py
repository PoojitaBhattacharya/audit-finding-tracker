from flask import Blueprint, jsonify
from services.groq_client import GroqClient

bp = Blueprint("health", __name__)

client = GroqClient()

@bp.route("/health", methods=["GET"])
def health():
    if client.response_times:
        avg_time = sum(client.response_times) / len(client.response_times)
    else:
        avg_time = 0

    return jsonify({
        "status": "ok",
        "model": client.model,
        "avg_response_time_ms": int(avg_time)
    })