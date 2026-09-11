(function(root){
class FocusTimer {
 constructor(state={}) { this.mode=['focus','break','stopwatch'].includes(state.mode)?state.mode:'focus';this.duration=Number.isFinite(state.duration)&&state.duration>0?state.duration:1500000;this.elapsed=Number.isFinite(state.elapsed)&&state.elapsed>=0?state.elapsed:0;this.startedAt=Number.isFinite(state.startedAt)?state.startedAt:null; }
 running(){return this.startedAt!==null;}
 value(now=Date.now()){return this.elapsed+(this.running()?Math.max(0,now-this.startedAt):0);}
 remaining(now=Date.now()){return this.mode==='stopwatch'?this.value(now):Math.max(0,this.duration-this.value(now));}
 start(now=Date.now()){if(!this.running())this.startedAt=now;}
 pause(now=Date.now()){this.elapsed=this.value(now);this.startedAt=null;}
 reset(){this.elapsed=0;this.startedAt=null;}
 done(now=Date.now()){return this.mode!=='stopwatch'&&this.value(now)>=this.duration;}
 snapshot(){return {mode:this.mode,duration:this.duration,elapsed:this.elapsed,startedAt:this.startedAt};}
}
if(typeof module!=='undefined')module.exports=FocusTimer;else root.FocusTimer=FocusTimer;
})(globalThis);
