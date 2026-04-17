use crate::errors::ValidationError;
use regex::Regex;
use std::sync::OnceLock;

fn org_re() -> &'static Regex {
    static R: OnceLock<Regex> = OnceLock::new();
    R.get_or_init(|| Regex::new(r"^[a-zA-Z0-9_-]+$").expect("ORG_RE"))
}

fn ref_re() -> &'static Regex {
    static R: OnceLock<Regex> = OnceLock::new();
    R.get_or_init(|| Regex::new(r"^[A-Z0-9_]+$").expect("REF_RE"))
}

pub fn validate_organization_id(s: &str) -> Result<(), ValidationError> {
    if s.is_empty() {
        return Err(ValidationError::Message(
            "organizationId must not be empty".into(),
        ));
    }
    if !org_re().is_match(s) {
        return Err(ValidationError::Message(
            "organizationId must contain only letters, digits, hyphens, and underscores".into(),
        ));
    }
    Ok(())
}

pub fn validate_reference_type(s: &str) -> Result<(), ValidationError> {
    if s.is_empty() {
        return Err(ValidationError::Message(
            "referenceType must not be empty".into(),
        ));
    }
    if !ref_re().is_match(s) {
        return Err(ValidationError::Message(
            "referenceType must be uppercase letters, digits, and underscores only".into(),
        ));
    }
    Ok(())
}

pub fn validate_path_segment(seg: &str, name: &str) -> Result<(), ValidationError> {
    if seg.is_empty() {
        return Err(ValidationError::Message(format!("{name} must not be empty")));
    }
    if seg.contains("..") || seg.contains('/') || seg.contains('\\') {
        return Err(ValidationError::Message(format!(
            "{name} must not contain path traversal sequences or separators"
        )));
    }
    Ok(())
}

pub fn file_key(org_id: &str, ref_type: &str, file_id: &str) -> String {
    format!("{org_id}/{ref_type}/{file_id}")
}
