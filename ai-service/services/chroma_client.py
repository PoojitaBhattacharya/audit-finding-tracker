import chromadb
from sentence_transformers import SentenceTransformer

class ChromaClient:

    def __init__(self):
        self.client = chromadb.Client()

        self.collection = self.client.get_or_create_collection(
            name="audit_docs"
        )

        self.model = SentenceTransformer("all-MiniLM-L6-v2")

    def add_documents(self, docs):
        embeddings = self.model.encode(docs).tolist()

        ids = [str(i) for i in range(len(docs))]

        self.collection.add(
            documents=docs,
            embeddings=embeddings,
            ids=ids
        )

    def query(self, text, n_results=3):
        embedding = self.model.encode([text]).tolist()

        results = self.collection.query(
            query_embeddings=embedding,
            n_results=n_results
        )

        return results["documents"]