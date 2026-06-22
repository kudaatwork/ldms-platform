-- Marketing feature lists for default subscription packages (one bullet per line on landing page).

UPDATE subscription_package
SET description = 'Essential platform access
Standard usage tracking
Best for small teams'
WHERE code = 'STARTER'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET description = 'Higher volume corridor programmes
Priority support from LX operations
Usage tracked for reporting transparency'
WHERE code = 'GROWTH'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET description = 'Enterprise-scale corridor operations
Dedicated customer success manager
Unlimited tracked platform actions'
WHERE code = 'ENTERPRISE'
  AND entity_status <> 'DELETED';
