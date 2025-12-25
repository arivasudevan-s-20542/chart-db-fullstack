import { useContext } from 'react';
import { authContext } from './auth-context';

export const useAuth = () => useContext(authContext);

export default useAuth;
