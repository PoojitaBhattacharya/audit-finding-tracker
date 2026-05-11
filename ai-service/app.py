from flask import Flask
from routes.categorise import bp as categorise_bp
from routes.query import bp as query_bp
from routes.health import bp as health_bp
from routes.describe import describe_bp
from routes.report import report_bp
from config import limiter
from services.sanitizer import sanitize_request
from services.response_masker import ResponseMasker
import json

app = Flask(__name__)

limiter.init_app(app)

app.register_blueprint(categorise_bp)
app.register_blueprint(query_bp)
app.register_blueprint(health_bp)
app.register_blueprint(describe_bp)
app.register_blueprint(report_bp)

# Wire input sanitization to run before every POST/PUT request
app.before_request(sanitize_request)

@app.after_request
def apply_security_headers(response):
    # Security Headers
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    response.headers['Referrer-Policy'] = 'strict-origin-when-cross-origin'
    
    # Content Security Policy - no unsafe-inline or unsafe-eval
    response.headers['Content-Security-Policy'] = (
        "default-src 'self'; "
        "script-src 'self'; "
        "style-src 'self'; "
        "img-src 'self' data:; "
        "font-src 'self'; "
        "connect-src 'self'; "
        "frame-ancestors 'none'; "
        "form-action 'self'; "
        "base-uri 'self'"
    )
    
    # Cache Control for sensitive data
    response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, max-age=0'
    response.headers['Pragma'] = 'no-cache'
    response.headers['Expires'] = '0'
    
    # Mask PII in response body if it's JSON
    if response.content_type and 'application/json' in response.content_type:
        try:
            response.data = ResponseMasker.mask_json_response(response.get_data(as_text=True)).encode('utf-8')
        except Exception as e:
            # Log error but don't break response
            print(f"[WARNING] Failed to mask PII in response: {str(e)}")
    
    return response

@app.route("/")
def home():
    return {"message": "AI Service Running"}

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)
