import { AlertCircle, CheckCircle2, LoaderCircle, SearchX } from 'lucide-react'
export function Spinner(){return <LoaderCircle className="spin" size={20}/>} 
export function Loading({label='Loading…'}:{label?:string}){return <div className="state"><Spinner/><span>{label}</span></div>}
export function ErrorState({message,action}:{message:string;action?:React.ReactNode}){return <div className="state error"><AlertCircle size={28}/><strong>{message}</strong>{action}</div>}
export function EmptyState({message='Nothing here yet.'}:{message?:string}){return <div className="state"><SearchX size={28}/><span>{message}</span></div>}
export function Success({children}:{children:React.ReactNode}){return <div className="notice success"><CheckCircle2 size={18}/>{children}</div>}
export function StatusBadge({children}:{children:React.ReactNode}){return <span className={`badge badge-${String(children).toLowerCase().replace(/[^a-z0-9]+/g,'-')}`}>{children}</span>}
