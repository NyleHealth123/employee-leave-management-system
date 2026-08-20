export type Role = 'EMPLOYEE' | 'MANAGER' | 'ADMINISTRATOR'
export interface Employee { id:string; employeeNumber:string; displayName:string; email:string; managerId:string|null; managerName:string|null; roles:Role[]; active:boolean; version:number }
export interface EmployeePage { content:Employee[]; page:number; size:number; totalElements:number; totalPages:number }
export interface LeavePolicy { id:string; versionNumber:number; effectiveFrom:string; effectiveTo:string|null; tracksBalance:boolean; allowsHalfDay:boolean; weeklyOffTreatment:'EXCLUDE'|'INCLUDE'; holidayTreatment:'EXCLUDE'|'INCLUDE'; rejectionCommentRequired:boolean; cancellationCutoffDays:number; weeklyOffDays:number[] }
export interface LeaveTypeDetail { id:string; code:string; name:string; tracksBalance:boolean; allowsHalfDay:boolean; cancellationCutoffDays:number; description:string|null; active:boolean; currentPolicy:LeavePolicy|null; version:number }
export interface Holiday { id:string; date:string; name:string; active:boolean; version:number }
export interface Balance { id:string; leaveTypeId:string; leaveTypeName:string; periodStart:string; periodEnd:string; entitledDays:number; reservedDays:number; consumedDays:number; availableDays:number; version:number }
