from flask import Flask
from routes.categorise import bp as categorise_bp
from routes.query import bp as query_bp

app = Flask(__name__)

app.register_blueprint(categorise_bp)
app.register_blueprint(query_bp)

@app.route("/")
def home():
    return {"message": "AI Service Running"}

if __name__ == "__main__":
    app.run(port=5000, debug=True)