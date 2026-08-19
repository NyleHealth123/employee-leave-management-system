import { http, HttpResponse } from 'msw'
import { api, onSessionExpired, resetApiClientForTests } from './apiClient'
import { server } from '../../test/setup'
import { afterEach, describe, expect, it, vi } from 'vitest'
afterEach(resetApiClientForTests)
describe('api client',()=>{
 it('sends cookies, obtains CSRF, and injects the header for unsafe requests',async()=>{let token='';server.use(http.get('/api/auth/csrf',()=>HttpResponse.json({token:'csrf-value',headerName:'X-XSRF-TOKEN'})),http.post('/api/example',({request})=>{token=request.headers.get('X-XSRF-TOKEN')??'';return HttpResponse.json({ok:true})}));await api('/example',{method:'POST',body:'{}'});expect(token).toBe('csrf-value')})
 it('maps problems and notifies on expired sessions',async()=>{const expired=vi.fn();onSessionExpired(expired);server.use(http.get('/api/private',()=>HttpResponse.json({type:'about:blank',title:'Unauthorized',status:401,code:'AUTHENTICATION_REQUIRED',detail:'Expired',correlationId:'c1'},{status:401})));await expect(api('/private')).rejects.toThrow('Expired');expect(expired).toHaveBeenCalledOnce()})
})

