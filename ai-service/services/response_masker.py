import re
import json
from typing import Any, Dict

class ResponseMasker:
    """
    Detects and masks Personally Identifiable Information (PII) in response bodies
    to prevent accidental disclosure of sensitive data.
    """
    
    # Patterns for various PII types
    CC_PATTERN = re.compile(r'\b(?:\d[ -]*?){13,16}\b')  # Credit cards
    SSN_PATTERN = re.compile(r'\b(?:\d{3}-?\d{2}-?\d{4})\b')  # Social Security Numbers
    EMAIL_PATTERN = re.compile(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b')  # Email
    PHONE_PATTERN = re.compile(r'\b(?:\+?1[-.]?)?\(?([0-9]{3})\)?[-.]?([0-9]{3})[-.]?([0-9]{4})\b')  # Phone
    DOB_PATTERN = re.compile(r'\b(0[1-9]|1[0-2])[-/](0[1-9]|[12][0-9]|3[01])[-/](19|20)\d{2}\b')  # DOB
    PASSPORT_PATTERN = re.compile(r'\b[A-Z]{1,2}\d{6,9}\b')  # Passport numbers
    
    @staticmethod
    def mask_credit_cards(text: str) -> str:
        """Mask credit card numbers (shows only last 4 digits)"""
        def replacer(match):
            digits_only = re.sub(r'[^\d]', '', match.group())
            if 13 <= len(digits_only) <= 16:
                return f"****-****-****-{digits_only[-4:]}"
            return match.group()
        return ResponseMasker.CC_PATTERN.sub(replacer, text)
    
    @staticmethod
    def mask_ssn(text: str) -> str:
        """Mask SSN (shows only last 4 digits)"""
        def replacer(match):
            ssn = match.group().replace('-', '')
            return f"***-**-{ssn[-4:]}"
        return ResponseMasker.SSN_PATTERN.sub(replacer, text)
    
    @staticmethod
    def mask_email(text: str) -> str:
        """Mask email addresses (shows first and last char of local part)"""
        def replacer(match):
            email = match.group()
            parts = email.split('@')
            local = parts[0]
            domain = parts[1]
            masked_local = local[0] + '***' + (local[-1] if len(local) > 1 else '')
            return f"{masked_local}@{domain}"
        return ResponseMasker.EMAIL_PATTERN.sub(replacer, text)
    
    @staticmethod
    def mask_phone(text: str) -> str:
        """Mask phone numbers (shows only last 4 digits)"""
        def replacer(match):
            digits = re.sub(r'[^\d]', '', match.group())
            return f"***-***-{digits[-4:]}"
        return ResponseMasker.PHONE_PATTERN.sub(replacer, text)
    
    @staticmethod
    def mask_dob(text: str) -> str:
        """Mask date of birth"""
        return ResponseMasker.DOB_PATTERN.sub(r'**/**/****', text)
    
    @staticmethod
    def mask_passport(text: str) -> str:
        """Mask passport numbers"""
        def replacer(match):
            passport = match.group()
            return f"{passport[0]}****{passport[-2:] if len(passport) > 2 else ''}"
        return ResponseMasker.PASSPORT_PATTERN.sub(replacer, text)
    
    @staticmethod
    def mask_all_pii(text: str) -> str:
        """Apply all PII masking rules in sequence"""
        if not isinstance(text, str):
            return text
        
        text = ResponseMasker.mask_credit_cards(text)
        text = ResponseMasker.mask_ssn(text)
        text = ResponseMasker.mask_email(text)
        text = ResponseMasker.mask_phone(text)
        text = ResponseMasker.mask_dob(text)
        text = ResponseMasker.mask_passport(text)
        
        return text
    
    @staticmethod
    def mask_dict_values(data: Dict[str, Any]) -> Dict[str, Any]:
        """Recursively mask PII in dictionary values"""
        if not isinstance(data, dict):
            return data
        
        masked_data = {}
        for key, value in data.items():
            if isinstance(value, str):
                masked_data[key] = ResponseMasker.mask_all_pii(value)
            elif isinstance(value, dict):
                masked_data[key] = ResponseMasker.mask_dict_values(value)
            elif isinstance(value, list):
                masked_data[key] = [
                    ResponseMasker.mask_dict_values(item) if isinstance(item, dict) else
                    ResponseMasker.mask_all_pii(item) if isinstance(item, str) else
                    item
                    for item in value
                ]
            else:
                masked_data[key] = value
        
        return masked_data
    
    @staticmethod
    def mask_json_response(json_str: str) -> str:
        """Mask PII in JSON response strings"""
        try:
            data = json.loads(json_str)
            masked_data = ResponseMasker.mask_dict_values(data)
            return json.dumps(masked_data)
        except (json.JSONDecodeError, TypeError):
            # If not valid JSON, apply string masking
            return ResponseMasker.mask_all_pii(json_str)
