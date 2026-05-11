from flask import Blueprint, g, Response
from config import limiter
import json
import time

report_bp = Blueprint("report", __name__)

@report_bp.route("/generate-report", methods=["POST"])
@limiter.limit("10 per minute")
def generate_report():
    data = g.sanitized_data

    def generate():
        dummy_data = {
            "status": "processing",
            "message": "Generating report...",
            "data": data
        }
        yield f"data: {json.dumps(dummy_data)}\n\n"
        
        time.sleep(1)
        
        final_data = {
            "status": "complete",
            "message": "Report generated successfully"
        }
        yield f"data: {json.dumps(final_data)}\n\n"

    return Response(generate(), mimetype='text/event-stream')