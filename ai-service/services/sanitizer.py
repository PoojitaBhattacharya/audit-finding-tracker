import re
from flask import request, jsonify, g
from html import escape as html_escape

# ============ PROMPT INJECTION PATTERNS ============
# Expanded prompt injection patterns (case-insensitive)
PROMPT_INJECTION_PATTERNS = [
    r"ignore\s+previous\s+instructions",
    r"act\s+as\s+system",
    r"bypass\s+restrictions",
    r"you\s+are\s+now",
    r"forget\s+all\s+rules",
    r"reveal\s+.*prompt",
    r"system\s+prompt",
    r"debug\s+mode",
    r"escape\s+this",
    r"disregard",
    r"override",
    r"jailbreak",
]

# ============ XSS PATTERNS ============
HTML_TAG_PATTERN = re.compile(r"<.*?>")
JAVASCRIPT_PATTERN = re.compile(r"javascript\s*:", re.IGNORECASE)
EVENT_HANDLER_PATTERN = re.compile(r"on\w+\s*=", re.IGNORECASE)
DANGEROUS_ELEMENTS = re.compile(
    r"<(script|iframe|object|embed|applet|meta|link|style|form|input|button|select|textarea|frame|frameset)[\s>]",
    re.IGNORECASE
)

# Input validation constraints
MAX_INPUT_LENGTH = 2000
ALLOWED_PATTERN = re.compile(r"^[a-zA-Z0-9\s.,!?@#()\-_:;/]*$")


def is_safe_input(text: str) -> bool:
    """Check if input matches allowed pattern"""
    if not text or len(text) == 0:
        return False
    return bool(ALLOWED_PATTERN.match(text))


def sanitize_text(text: str) -> str:
    """Remove HTML tags and trim whitespace"""
    text = re.sub(HTML_TAG_PATTERN, "", text)
    return text.strip()


def encode_for_html(text: str) -> str:
    """Encode text for safe insertion in HTML"""
    return html_escape(text, quote=True)


def encode_for_html_attribute(text: str) -> str:
    """Encode text for safe use in HTML attributes"""
    # Escape quotes, single quotes, and angle brackets
    text = text.replace("&", "&amp;")
    text = text.replace('"', "&quot;")
    text = text.replace("'", "&#x27;")
    text = text.replace("<", "&lt;")
    text = text.replace(">", "&gt;")
    text = text.replace("/", "&#x2F;")
    return text


def encode_for_javascript(text: str) -> str:
    """Encode text for safe use in JavaScript strings"""
    # Escape special characters for JavaScript
    text = text.replace("\\", "\\\\")
    text = text.replace('"', '\\"')
    text = text.replace("'", "\\'")
    text = text.replace("\n", "\\n")
    text = text.replace("\r", "\\r")
    text = text.replace("\t", "\\t")
    text = text.replace("\b", "\\b")
    text = text.replace("\f", "\\f")
    return text


def encode_for_url(text: str) -> str:
    """URL encode text"""
    from urllib.parse import quote
    return quote(text, safe='')


def contains_prompt_injection(text: str) -> bool:
    """Detect known prompt injection patterns"""
    text_lower = text.lower()
    for pattern in PROMPT_INJECTION_PATTERNS:
        if re.search(pattern, text_lower):
            return True
    return False


def contains_xss_vectors(text: str) -> bool:
    """Detect common XSS attack vectors"""
    # Check for HTML tags
    if HTML_TAG_PATTERN.search(text):
        return True
    
    # Check for JavaScript protocol
    if JAVASCRIPT_PATTERN.search(text):
        return True
    
    # Check for event handlers
    if EVENT_HANDLER_PATTERN.search(text):
        return True
    
    # Check for dangerous HTML elements
    if DANGEROUS_ELEMENTS.search(text):
        return True
    
    # Check for encoded XSS attempts
    dangerous_patterns = [
        r"&#\d+;",  # HTML entities
        r"&#x[0-9a-fA-F]+;",  # Hex entities
        r"\\u[0-9a-fA-F]{4}",  # Unicode escapes
        r"%3[Cc]",  # Encoded <
        r"%3[Ee]",  # Encoded >
    ]
    
    for pattern in dangerous_patterns:
        if re.search(pattern, text):
            return True
    
    return False


def sanitize_all_fields(data: dict) -> tuple:
    """
    Sanitize and validate all fields in request data.
    
    Returns:
        Tuple of (is_valid: bool, result: dict or error_message: str)
    """
    if not data:
        return False, "Empty request body"

    if not isinstance(data, dict):
        return False, "Invalid JSON format"

    sanitized_data = {}
    
    for key, value in data.items():
        if isinstance(value, str):
            # Validate string length
            if len(value.strip()) == 0:
                return False, f"Field '{key}' cannot be empty"

            if len(value) > MAX_INPUT_LENGTH:
                return False, f"Field '{key}' exceeds maximum length of {MAX_INPUT_LENGTH}"

            # Check for prompt injection
            if contains_prompt_injection(value):
                print(f"[SECURITY] Prompt injection attempt detected in field: {key}")
                return False, f"Potentially malicious content detected in '{key}'"

            # Check for XSS vectors
            if contains_xss_vectors(value):
                print(f"[SECURITY] XSS vector detected in field: {key}")
                return False, f"Invalid characters or HTML detected in '{key}'"

            # Check against allowed pattern (if not XSS)
            if not is_safe_input(value):
                print(f"[SECURITY] Unsafe input detected in field: {key}")
                return False, f"Field '{key}' contains invalid characters"

            # Sanitize the text
            sanitized_data[key] = sanitize_text(value)

        elif isinstance(value, (int, float, bool)):
            # Allow numeric and boolean values
            sanitized_data[key] = value
        
        elif isinstance(value, list):
            # Recursively sanitize list items
            sanitized_list = []
            for item in value:
                if isinstance(item, str):
                    if contains_prompt_injection(item) or contains_xss_vectors(item):
                        return False, f"Invalid content in list field '{key}'"
                    sanitized_list.append(sanitize_text(item))
                else:
                    sanitized_list.append(item)
            sanitized_data[key] = sanitized_list
        
        elif isinstance(value, dict):
            # Recursively sanitize nested dictionaries
            is_valid, result = sanitize_all_fields(value)
            if not is_valid:
                return False, f"Error in nested field '{key}': {result}"
            sanitized_data[key] = result
        
        else:
            # Allow other types as-is
            sanitized_data[key] = value

    return True, sanitized_data


def sanitize_request():
    """
    Flask before_request hook to sanitize incoming requests.
    Validates and sanitizes POST/PUT request bodies.
    """
    if request.method in ["POST", "PUT", "PATCH"]:
        data = request.get_json(silent=True)

        is_valid, result = sanitize_all_fields(data)

        if not is_valid:
            return jsonify({"error": result}), 400

        # Store sanitized data for access in route handlers
        g.sanitized_data = result
        print(f"[INFO] Request sanitized successfully ({request.method} {request.path})")