from flask import Flask
from routes.categorise import bp as categorise_bp
from routes.query import bp as query_bp
from routes.health import bp as health_bp
from routes.describe import describe_bp
from routes.report import report_bp
from config import limiter
from services.sanitizer import sanitize_request

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
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Content-Security-Policy'] = "default-src 'self'; script-src 'self'; style-src 'self'; frame-ancestors 'none'; form-action 'self';"
    response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, max-age=0'
    response.headers['Pragma'] = 'no-cache'
    return response

@app.route("/")
def home():
    return {"message": "AI Service Running"}

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)