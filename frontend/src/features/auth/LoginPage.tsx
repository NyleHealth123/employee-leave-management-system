import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../../app/providers/AuthProvider'
import { ErrorSummary } from '../../shared/components/Ui'
export function LoginPage() { const auth=useAuth(); const [login,setLogin]=useState('');const [password,setPassword]=useState('');const [error,setError]=useState('');if(auth.principal)return <Navigate to="/employee" replace/>;async function submit(e:FormEvent){e.preventDefault();setError('');try{await auth.login(login,password)}catch(ex){setError(ex instanceof Error?ex.message:'Unable to sign in')}}return <main className="login"><h1>Employee leave management</h1>{error&&<ErrorSummary message={error}/>}<form onSubmit={submit}><label>Login<input required value={login} onChange={e=>setLogin(e.target.value)}/></label><label>Password<input required type="password" value={password} onChange={e=>setPassword(e.target.value)}/></label><button>Sign in</button></form></main> }

