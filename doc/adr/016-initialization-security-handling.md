# ADR-016: Initialization Security Handling

## Status
Accepted

## Context
Many upstream repositories contain secrets or sensitive data in their git history. GitHub's push protection feature blocks these commits from being pushed, which prevents the initialization workflow from creating the `fork_upstream` branch during repository setup.

## Decision
We will temporarily disable security features during the initialization process and re-enable them immediately after completion. This approach:

1. Disables push protection at the start of initialization
2. Allows the upstream sync to complete successfully
3. Re-enables all security features before closing the initialization issue

## Implementation
The implementation is straightforward:

```yaml
# Early in init-complete.yml
- name: Disable Security Features
  env:
    GH_TOKEN: ${{ secrets.GH_TOKEN }}
  run: |
    if [ -n "$GH_TOKEN" ]; then
      gh api --method PATCH "/repos/${{ github.repository }}" \
        --input .github/security-off.json
    fi

# ... initialization steps ...

# At the end of init-complete.yml
- name: Re-enable Security Features
  env:
    GH_TOKEN: ${{ secrets.GH_TOKEN }}
  run: |
    if [ -n "$GH_TOKEN" ]; then
      gh api --method PATCH "/repos/${{ github.repository }}" \
        --input .github/security-on.json
    fi
```

## Consequences

### Positive
- Simple and maintainable solution
- No complex error handling required
- Works for all upstream repositories regardless of their history
- Security is only disabled during initialization, not during normal operations

### Negative
- Requires `GH_TOKEN` with admin permissions for full functionality
- Brief window where push protection is disabled (during initialization only)

### Security Considerations
- The temporary disable only affects the initialization process
- All future operations have full security protection enabled
- Existing secrets in upstream history are already public
- This approach doesn't introduce new security risks

## Alternatives Considered
1. **Complex error handling**: Would require detecting specific push protection errors and creating issues for manual resolution
2. **Manual allowlisting**: Would require users to manually allow each secret through GitHub's UI
3. **History rewriting**: Would break synchronization with upstream

The chosen approach is the simplest and most reliable solution that maintains the integrity of the fork relationship while ensuring security for all future operations.