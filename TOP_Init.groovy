// PROP: scriptName=TOP_Init
// PROP: strict=false
// PROP: startup=true


def markHealers(dryRun=false) {
	List<XivPlayerCharacter> healerPlayers = state.partyList.stream()
		.filter(player -> player.job.healer)
		.sorted(Comparator.comparing(player -> [Job.WHM, Job.AST, Job.SCH, Job.SGE].indexOf(player.job)))
		.toList()

	//log.info("${healerPlayers}")
	
	if (!dryRun) {
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
		if (state.zoneIs(0x462)) {
			//s.waitMs(2000);
			//s.accept(new ClearAutoMarkRequest());
			s.waitMs(8000);
			markHealers();
		}
	}}
})

markHealers(dryRun=true);