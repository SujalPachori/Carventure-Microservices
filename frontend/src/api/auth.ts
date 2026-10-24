import { clients } from './client'
import type { AuthResponse, Role, UserRoles } from '../types'
export const authApi = {
  login: (payload:{username:string;password:string}) => clients.auth.post<AuthResponse>('/api/auth/login',payload).then(r=>r.data),
  register: (payload: {
  username: string;
  password: string;
  role: Role;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  address: string;
  pincode: string;
  dateOfBirth?: string;
}) =>
  clients.auth
    .post<AuthResponse>('/api/auth/register', payload)
    .then(r => r.data),
  refresh: (refreshToken:string) => clients.auth.post<AuthResponse>('/api/auth/refreshToken',{refreshToken}).then(r=>r.data),
  roles: (username:string) => clients.auth.get<UserRoles>(`/api/auth/admin/users/${encodeURIComponent(username)}/roles`).then(r=>r.data),
  addRole: (username:string,role:Role) => clients.auth.post<UserRoles>(`/api/auth/admin/users/${encodeURIComponent(username)}/roles`,{role}).then(r=>r.data)
}
