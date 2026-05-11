from flask import Blueprint, jsonify
from services.groq_client import GroqClient

bp = Blueprint("health", __name__)

_client = None

def get_client():
    global _client
    if _client is None:
        _client = GroqClient()
    return _client

@bp.route("/health", methods=["GET"])
def health():
    client = get_client()
    if client.response_times:
        avg_time = sum(client.response_times) / len(client.response_times)
    else:
        avg_time = 0

    return jsonify({
        "status": "ok",
        "model": client.model,
        "avg_response_time_ms": int(avg_time)
    })