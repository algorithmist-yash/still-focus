(function(root) {
 'use strict';
 const modes = ['focus', 'break', 'stopwatch'];
 const units = {hours:3600000, days:86400000, weeks:604800000};
 function range(count, unit, now=Date.now()) {
  if (!Number.isInteger(count) || count<1 || count>10000 || ![...Object.keys(units),'months','years'].includes(unit)) throw new Error('Choose a whole number from 1 to 10,000 and a time unit.');
  const date = new Date(now);
  if (units[unit]) return {start:now-count*units[unit], end:now};
  const day=date.getDate(); date.setDate(1);
  if(unit==='months') date.setMonth(date.getMonth()-count); else date.setFullYear(date.getFullYear()-count);
  const last = new Date(date.getFullYear(),date.getMonth()+1,0).getDate();date.setDate(Math.min(day,last));
  return {start:date.getTime(),end:now};
 }
 function segments(session, now=Date.now()) {
  const spans=(session.segments||[]).map(s=>[s[0],s[1]]);
  if(Number.isFinite(session.openAt)) spans.push([session.openAt,Math.max(session.openAt,now)]);
  let budget=session.mode==='stopwatch'?Infinity:session.target;
  return spans.map(([start,end])=>{const length=Math.min(Math.max(0,end-start),Math.max(0,budget));budget-=length;return [start,start+length];});
 }
 function duration(session,start=-Infinity,end=Infinity,now=Date.now()) {
  return segments(session,now).reduce((sum,[a,b])=>sum+Math.max(0,Math.min(b,end)-Math.max(a,start)),0);
 }
 class Ledger {
  constructor(state={}) { this.records=Array.isArray(state.records)?state.records:[];this.active=state.active||null; }
  begin(name,mode,target,now=Date.now()) {
   name=String(name||'').trim();if(!name||name.length>100)throw new Error('Enter a session name (up to 100 characters).');
   if(this.active)throw new Error('Finish the current session first.');
   if(!modes.includes(mode)||!Number.isFinite(target)||target<=0)throw new Error('Invalid clock.');
   this.active={id:root.crypto?.randomUUID?.()||`${now}-${Math.random().toString(36).slice(2)}`,name,mode,target,startedAt:now,segments:[],openAt:now,status:'running',laps:[]};return this.active;
  }
  pause(now=Date.now()) { const a=this.active;if(!a||a.openAt===null)return;if(Number.isFinite(a.openAt))a.segments.push([a.openAt,Math.max(a.openAt,now)]);a.openAt=null;a.status='paused'; }
  resume(now=Date.now()) { if(!this.active)throw new Error('Name a session first.');if(this.active.openAt===null){this.active.openAt=now;this.active.status='running';} }
  finish(status,now=Date.now(),laps=[]) {
   if(!this.active)return null;
   this.pause(now);const a=this.active;a.segments=segments(a,now);a.endedAt=now;a.status=status;a.laps=[...laps];a.duration=duration(a);this.records.push(a);this.active=null;return a;
  }
  all(){return this.active?[...this.records,this.active]:this.records;}
  snapshot(){return {records:this.records,active:this.active};}
 }
 function migrate(history=[]) {return history.filter(h=>Number.isFinite(h.at)&&Number.isFinite(h.duration)&&h.duration>0).map((h,i)=>({id:`legacy-${h.at}-${i}`,name:h.intention?.trim()||'Earlier focus session',mode:'focus',target:h.duration,startedAt:h.at-h.duration,endedAt:h.at,segments:[[h.at-h.duration,h.at]],openAt:null,status:'completed',duration:h.duration,laps:[],estimated:true}));}
 function analyze(records,start,end,mode='all',now=Date.now()) {
  if(!Number.isFinite(start)||!Number.isFinite(end)||start>=end)throw new Error('Choose an end after the start.');
  const rows=records.filter(s=>mode==='all'||s.mode===mode).map(s=>({...s,inRange:duration(s,start,end,now),total:duration(s,-Infinity,Infinity,now)})).filter(s=>s.inRange>0).sort((a,b)=>b.startedAt-a.startedAt);
  const byMode={focus:0,break:0,stopwatch:0};const groups=new Map();
  for(const s of rows){byMode[s.mode]+=s.inRange;const key=s.name.trim().toLocaleLowerCase();const g=groups.get(key)||{name:s.name,time:0,count:0};g.time+=s.inRange;g.count++;groups.set(key,g);}
  const total=rows.reduce((sum,s)=>sum+s.inRange,0);
  return {rows,total,byMode,count:rows.length,average:rows.length?total/rows.length:0,completed:rows.filter(s=>s.status==='completed'||s.status==='finished').length,groups:[...groups.values()].sort((a,b)=>b.time-a.time)};
 }
 function buckets(records,start,end,group='auto',mode='all',now=Date.now()) {
  const widths={hour:3600000,day:86400000,week:604800000,month:2592000000,year:31536000000};
  if(group==='auto')group=end-start<=2*86400000?'hour':end-start<=90*86400000?'day':end-start<=2*31536000000?'month':'year';
  if(!widths[group])throw new Error('Choose a chart interval.');
  // Keep long ranges readable; all records still contribute to totals and groups.
  const step=Math.max(1,Math.ceil((end-start)/widths[group]/120));
  const list=[];let at=start;
  while(at<end&&list.length<122){let next;if(group==='month'||group==='year'){const d=new Date(at);const day=d.getDate();d.setDate(1);if(group==='month')d.setMonth(d.getMonth()+step);else d.setFullYear(d.getFullYear()+step);d.setDate(Math.min(day,new Date(d.getFullYear(),d.getMonth()+1,0).getDate()));next=d.getTime();}else next=at+widths[group]*step;next=Math.min(end,next);if(next<=at)break;const sums={focus:0,break:0,stopwatch:0};for(const s of records){if(mode==='all'||mode===s.mode)sums[s.mode]+=duration(s,at,next,now);}list.push({start:at,end:next,...sums,total:sums.focus+sums.break+sums.stopwatch});at=next;}
  return {group,step,list};
 }
 const api={Ledger,range,segments,duration,migrate,analyze,buckets};if(typeof module!=='undefined')module.exports=api;else root.StillActivity=api;
})(globalThis);
