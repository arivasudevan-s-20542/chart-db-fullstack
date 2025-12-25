import { useContext } from 'react';
import { collaborationContext } from './collaboration-context';

export const useCollaboration = () => useContext(collaborationContext);

export default useCollaboration;
