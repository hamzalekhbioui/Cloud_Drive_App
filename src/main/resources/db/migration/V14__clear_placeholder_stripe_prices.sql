-- V14: remove non-deployable example price IDs from existing databases.
UPDATE plans
   SET stripe_price_id = NULL
 WHERE LOWER(stripe_price_id) = 'price_replace_me';
