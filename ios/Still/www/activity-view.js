let activityPageSize=50;
function activityTime(ms){const seconds=Math.floor(ms/1000),h=Math.floor(seconds/3600),m=Math.floor(seconds/60)%60,s=seconds%60;return h?`${h}h ${m}m ${s}s`:m?`${m}m ${s}s`:`${s}s`;}
function activityDate(at){return new Date(at).toLocaleString(undefined,{dateStyle:'medium',timeStyle:'short'});}
function activityRange(now){
 const value=$('#activity-range').value;
 if(value==='24h')return StillActivity.range(24,'hours',now);
 if(value==='week')return StillActivity.range(7,'days',now);
 if(value==='month')return StillActivity.range(1,'months',now);
 if(value==='year')return StillActivity.range(1,'years',now);
 if(value==='rolling')return StillActivity.range(Number($('#range-count').value),$('#range-unit').value,now);
 if(value==='custom'){
  const start=new Date($('#range-from').value).getTime(),end=new Date($('#range-to').value).getTime();
  if(!Number.isFinite(start)||!Number.isFinite(end)||start>=end)throw new Error('Choose valid dates, with the end after the start.');
  if(start>now)throw new Error('Choose a start time before now.');
  return {start,end:Math.min(end,now)};
 }
 return {start:ledger.all().reduce((earliest,s)=>Math.min(earliest,s.startedAt),now-1),end:now};
}
function renderActivity(){
 const expanded=[...document.querySelectorAll('#analysis-content details[open]')].map(el=>el.dataset.detail);
 const now=Date.now();let bounds;
 try{bounds=activityRange(now);if(bounds.start>=bounds.end)throw new Error('Choose a range with elapsed time.');}
 catch(e){$('#range-error').textContent=e.message;$('#range-error').hidden=false;$('#analysis-content').hidden=true;$('#range-description').textContent='';return;}
 $('#range-error').hidden=true;$('#analysis-content').hidden=false;
 const {start,end}=bounds,mode=$('#activity-mode').value;
 const report=StillActivity.analyze(ledger.all(),start,end,mode,now);
 const chart=StillActivity.buckets(ledger.all(),start,end,$('#activity-group').value,mode,now);
 $('#range-description').textContent=`${activityDate(start)} – ${activityDate(end)} · Saved on this device · Updates while a clock runs`;
 const metrics=[['Time tracked',activityTime(report.total)],['Sessions in range',report.count],['Average tracked time',activityTime(report.average)],['Completed / finished',report.completed]];
 $('#analysis-summary').innerHTML=metrics.map(([label,value])=>`<div><span>${label}</span><strong>${value}</strong></div>`).join('');
 $('#chart-caption').textContent=`${chart.step>1?chart.step+' ':''}${chart.group}${chart.step>1?'s':''} per bar · From range start`;
 const max=Math.max(1,...chart.list.map(b=>b.total));
 $('#activity-chart').innerHTML=report.rows.length?`<div class="bar-chart" role="img" aria-label="Tracked time by ${chart.group}. Total ${escapeHTML(activityTime(report.total))}. Exact values are available in the chart data table.">${chart.list.map(b=>`<div class="chart-column" title="${escapeHTML(activityDate(b.start)+' – '+activityDate(b.end)+': '+activityTime(b.total))}"><div class="chart-stack" style="height:${Math.max(b.total?1:0,b.total/max*100)}%"><span class="bar-stopwatch" style="flex:${b.stopwatch}"></span><span class="bar-break" style="flex:${b.break}"></span><span class="bar-focus" style="flex:${b.focus}"></span></div></div>`).join('')}</div><div class="chart-axis"><span>${escapeHTML(activityDate(start))}</span><span>${escapeHTML(activityDate(end))}</span></div><details class="chart-data"><summary>View exact chart values</summary><div class="table-scroll"><table><thead><tr><th>Interval starting</th><th>Focus</th><th>Break</th><th>Stopwatch</th></tr></thead><tbody>${chart.list.map(b=>`<tr><td>${escapeHTML(activityDate(b.start))}</td><td>${activityTime(b.focus)}</td><td>${activityTime(b.break)}</td><td>${activityTime(b.stopwatch)}</td></tr>`).join('')}</tbody></table></div></details>`:'<div class="analysis-empty">No time tracked in this range.<br><span>Start a named session, or choose a different time range.</span></div>';
 const groupMax=Math.max(1,...report.groups.map(g=>g.time));
 $('#activity-groups').innerHTML=report.groups.length?report.groups.map(g=>`<div class="activity-group"><div><strong>${escapeHTML(g.name)}</strong><span>${activityTime(g.time)}</span></div><div class="group-track"><span style="width:${g.time/groupMax*100}%"></span></div><small>${g.count} session${g.count===1?'':'s'} · ${Math.round(g.time/report.total*100)}% of tracked time</small></div>`).join(''):'<p class="analysis-note">Your named sessions will appear here.</p>';
 const labels={focus:'Focus timer',break:'Break timer',stopwatch:'Stopwatch'};
 $('#activity-types').innerHTML=Object.entries(report.byMode).map(([key,time])=>`<div class="activity-type"><span class="type-dot ${key}"></span><span>${labels[key]}</span><strong>${activityTime(time)}</strong></div>`).join('');
 $('#history-list').innerHTML=report.rows.length?`<div class="table-scroll"><table class="session-table"><thead><tr><th>Session / event</th><th>Clock</th><th>Started</th><th>In this range</th><th>Status</th></tr></thead><tbody>${report.rows.slice(0,activityPageSize).map(s=>`<tr><td><strong>${escapeHTML(s.name)}</strong><small>Total running time: ${activityTime(s.total)}${s.estimated?' · Estimated timing':''}</small>${s.endedAt?`<small>Ended ${escapeHTML(activityDate(s.endedAt))}</small>`:''}${s.laps?.length?`<details><summary>${s.laps.length} stopwatch laps</summary>${s.laps.map((lap,i)=>`<div>Lap ${i+1}: ${activityTime(lap)} total · ${activityTime(lap-(s.laps[i-1]||0))} split</div>`).join('')}</details>`:''}</td><td>${labels[s.mode]}</td><td>${escapeHTML(activityDate(s.startedAt))}</td><td>${activityTime(s.inRange)}</td><td><span class="session-state ${s.status}">${escapeHTML(s.status)}</span></td></tr>`).join('')}</tbody></table></div>`:'<p class="analysis-note">No sessions match your filters.</p>';
 $('#more-sessions').hidden=report.rows.length<=activityPageSize;
 document.querySelectorAll('#analysis-content details').forEach((el,i)=>{el.dataset.detail=String(i);el.open=expanded.includes(String(i));});
}
function setupActivityFilters(){
 const filterIds=['activity-range','range-count','range-unit','range-from','range-to','activity-mode','activity-group'];
 const saved=read('still-analysis-filters',{});
 const localInput=at=>{const d=new Date(at);return new Date(at-d.getTimezoneOffset()*60000).toISOString().slice(0,16);};
 $('#range-from').value=localInput(Date.now()-86400000);$('#range-to').value=localInput(Date.now());
 for(const id of filterIds){if(typeof saved[id]==='string'){$('#'+id).value=saved[id];if($('#'+id).tagName==='SELECT'&&!$('#'+id).value)$('#'+id).selectedIndex=0;}$('#'+id).onchange=()=>{activityPageSize=50;updateFields();try{localStorage.setItem('still-analysis-filters',JSON.stringify(Object.fromEntries(filterIds.map(key=>[key,$('#'+key).value]))));}catch{}renderActivity();};}
 function updateFields(){$('#rolling-fields').hidden=$('#activity-range').value!=='rolling';$('#custom-range-fields').hidden=$('#activity-range').value!=='custom';}
 updateFields();$('#more-sessions').onclick=()=>{activityPageSize+=50;renderActivity();};
}
