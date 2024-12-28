// PROP: scriptName=TOP_Init
// PROP: strict=false
// PROP: startup=true

def TOP_Enter_AM(s) {
	//log.info("init");
	s.waitMs(8000);

	if (state.zoneIs(0x462)) {
		List<XivPlayerCharacter> healerPlayers = state.getPartyList().stream()
			.filter(player -> player.job.healer)
			.sorted(Comparator.comparing(player -> [Job.WHM, Job.AST, Job.SCH, Job.SGE].indexOf(player.job)))
			.toList()
	
		log.info("Healers: ${healerPlayers}")
		
		if (healerPlayers.size() == 2) {
			eventMaster.pushEvent(new SpecificAutoMarkRequest(healerPlayers.get(0), MarkerSign.ATTACK1));
			eventMaster.pushEvent(new SpecificAutoMarkRequest(healerPlayers.get(1), MarkerSign.ATTACK2));
		}
	}
}

groovyTriggers.add({
	named "TOP_Enter"
	when { DutyCommenceEvent dce -> true }
	sequence { e1, s -> {
		TOP_Enter_AM(s);
	}}
});