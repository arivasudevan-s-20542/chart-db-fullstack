-- Fix message role constraint to match Java enum uppercase values
ALTER TABLE ai_messages DROP CONSTRAINT valid_message_role;
ALTER TABLE ai_messages ADD CONSTRAINT valid_message_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'));
