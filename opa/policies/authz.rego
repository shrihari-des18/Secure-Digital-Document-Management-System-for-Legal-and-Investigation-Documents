package authz

import rego.v1

# Default deny
default allow := false

# Role helpers
is_system_admin if "SYSTEM_ADMIN" in input.subject.roles
is_investigating_officer if "INVESTIGATING_OFFICER" in input.subject.roles
is_forensic_analyst if "FORENSIC_ANALYST" in input.subject.roles
is_prosecutor if "PROSECUTOR" in input.subject.roles
is_records_custodian if "RECORDS_CUSTODIAN" in input.subject.roles
is_court if "COURT" in input.subject.roles
is_defense if "DEFENSE" in input.subject.roles
is_auditor if "AUDITOR" in input.subject.roles

# ABAC Constraints
has_clearance if input.subject.clearanceLevel >= input.resource.classification
is_assigned_case if input.resource.caseId in input.subject.assignedCaseIds
is_discoverable if input.resource.isDiscoverable == true

# Sealing logic
is_not_sealed if input.resource.isSealed == false
is_not_sealed if {
    input.resource.isSealed == true
    is_court
}

# System Admin (Strictly configuration, no case/evidence access)
allow if {
    is_system_admin
    input.resource.type == "System_Config"
    input.action in {"Create", "Read", "Update_Metadata"}
}

# Investigating Officer
allow if {
    is_investigating_officer
    input.resource.type in {"Case", "Evidence"}
    input.action in {"Create", "Ingest", "Read", "View", "Download", "Update_Metadata"}
    has_clearance
    is_not_sealed
}
allow if {
    is_investigating_officer
    input.resource.type == "Custody_Event"
    input.action == "Initiate_Transfer"
    input.resource.currentCustodian == input.subject.id
}
allow if {
    is_investigating_officer
    input.resource.type == "Custody_Event"
    input.action == "Accept_Transfer"
}

# Forensic Analyst
allow if {
    is_forensic_analyst
    input.resource.type in {"AI_Analysis", "Certificate", "Evidence"}
    input.action in {"Execute", "Read", "View", "Download"}
    has_clearance
    is_assigned_case
    is_not_sealed
}

# Prosecutor
allow if {
    is_prosecutor
    input.resource.type in {"Case", "Evidence", "Certificate"}
    input.action in {"Read", "View", "Download"}
    has_clearance
    is_assigned_case
    is_not_sealed
}
allow if {
    is_prosecutor
    input.resource.type == "Legal_Document"
    input.action in {"Create", "Update_Metadata", "Read"}
    has_clearance
    is_assigned_case
    is_not_sealed
}

# Records Custodian
allow if {
    is_records_custodian
    input.resource.type in {"Case", "Certificate"}
    input.action in {"Execute", "Read", "Update_Metadata"}
    has_clearance
    is_not_sealed
}

# Court
allow if {
    is_court
    input.resource.type in {"Case", "Evidence", "Certificate", "Legal_Document"}
    input.action in {"Read", "View", "Download", "Update_Metadata", "Seal", "Unseal"}
    has_clearance
}

# Defense
allow if {
    is_defense
    input.resource.type in {"Case", "Evidence", "Certificate", "Legal_Document"}
    input.action in {"Read", "View"}
    is_assigned_case
    is_discoverable
    is_not_sealed
}

# Auditor
allow if {
    is_auditor
    input.resource.type in {"Audit_Ledger", "System_Log"}
    input.action == "Read"
}
