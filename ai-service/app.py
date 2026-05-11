from flask import Flask
from routes.categorise import bp as categorise_bp
from routes.query import bp as query_bp
from routes.health import bp as health_bp

app = Flask(__name__)

app.register_blueprint(categorise_bp)
app.register_blueprint(query_bp)
app.register_blueprint(health_bp)

@app.after_request
def apply_security_headers(response):
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Content-Security-Policy'] = "frame-ancestors 'none'; form-action 'self';"
    return response

@app.route("/")
def home():
    return {"message": "AI Service Running"}

if __name__ == "__main__":
    app.run(port=5000, debug=True)