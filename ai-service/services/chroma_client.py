import chromadb
from sentence_transformers import SentenceTransformer
import uuid

class ChromaClient:

    def __init__(self):
        self.client = chromadb.Client()

        self.collection = self.client.get_or_create_collection(
            name="audit_docs"
        )

        self.model = SentenceTransformer("all-MiniLM-L6-v2")

    def add_documents(self, docs):
        chunked_docs = []
        chunk_size = 500
        overlap = 50
        
        for doc in docs:
            start = 0
            doc_len = len(doc)
            
            if doc_len == 0:
                continue
                
            while start < doc_len:
                end = min(start + chunk_size, doc_len)
                chunked_docs.append(doc[start:end])
                
                if end == doc_len:
                    break
                    
                start += (chunk_size - overlap)

        if not chunked_docs:
            return

        embeddings = self.model.encode(chunked_docs).tolist()

        ids = [str(uuid.uuid4()) for _ in range(len(chunked_docs))]

        self.collection.add(
            documents=chunked_docs,
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