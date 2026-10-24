import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { authApi } from '../api/auth'
import type { AuthResponse, Role } from '../types'

type AuthContextValue = {auth:AuthResponse|null; login:(u:string,p:string)=>Promise<AuthResponse>; register: (payload: {
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
}) => Promise<AuthResponse>; logout:()=>void; hasRole:(r:Role|Role[])=>boolean; isAuthenticated:boolean}
const AuthContext=createContext<AuthContextValue|null>(null)
const load=()=>{try{return JSON.parse(localStorage.getItem('carventure_auth')||'null') as AuthResponse|null}catch{return null}}
export function AuthProvider({children}:{children:React.ReactNode}){
 const [auth,setAuth]=useState<AuthResponse|null>(load)
 useEffect(()=>{if(auth){localStorage.setItem('carventure_auth',JSON.stringify(auth))}},[auth])
 const save=(a:AuthResponse)=>{setAuth(a);localStorage.setItem('carventure_access_token',a.accessToken);localStorage.setItem('carventure_refresh_token',a.refreshToken);localStorage.setItem('carventure_auth',JSON.stringify(a));return a}
 const value=useMemo<AuthContextValue>(()=>({auth,isAuthenticated:!!auth?.accessToken,login:async(u,p)=>save(await authApi.login({username:u,password:p})),register: async (payload) =>
  save(await authApi.register(payload)),logout:()=>{setAuth(null);localStorage.removeItem('carventure_access_token');localStorage.removeItem('carventure_refresh_token');localStorage.removeItem('carventure_auth')},hasRole:(r)=>{const roles=auth?.roles instanceof Set?Array.from(auth.roles):auth?.roles||[];return Array.isArray(r)?r.some(x=>roles.includes(x)):roles.includes(r)}}),[auth])
 return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
export const useAuth=()=>{const c=useContext(AuthContext);if(!c)throw new Error('useAuth must be used within AuthProvider');return c}
