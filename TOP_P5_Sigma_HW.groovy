// PROP: scriptName=TOP_P5_Sigma_HW
// PROP: strict=false
// PROP: startup=true
import java.time.Duration;
import java.util.stream.Collectors;

psJobPrio = [
	Job.WHM, Job.AST, 
     Job.PLD, Job.WAR, Job.GNB, Job.DRK, 
     Job.NIN, Job.SAM, Job.MNK, Job.RPR, Job.DRG,
     Job.BRD, Job.MCH, Job.DNC, 
     Job.BLM, Job.RDM, Job.SMN, 
     Job.SCH, Job.SGE
]

groovyTriggers.add({	
	named "TOP_P5_Sigma_HW_AM"
	// Run: Dynamis (Sigma)
	when { AbilityCastStart acs -> acs.abilityIdMatches(0x8014) }  
	sequence { e1, s -> {
		// capture 8 PS markers
		List<HeadMarkerEvent> headMarkers = s.waitEventsQuickSuccession(8, HeadMarkerEvent.class, hme -> hme.getTarget().isPc(), Duration.ofSeconds(1));
		
		Map<XivPlayerCharacter, int> headmarkerMap = headMarkers.stream().collect(Collectors.toMap(hme -> XivPlayerCharacter.class::cast(hme.getTarget()), hme -> hme.getMarkerOffset()));
			
		log.info("[TOP_P5_Sigma_PS_AM] PS headmarkers map: {}", headmarkerMap);
		
		// capture 6 target markers
		List<HeadMarkerEvent> targetHeadmarkers = s.waitEvents(6, HeadMarkerEvent.class, hme -> true)//hme.getMarkerOffset() == 0x6D)
		List<XivPlayerCharacter> targetPlayers = targetHeadmarkers.stream().map(hme -> hme.getTarget()).map(XivPlayerCharacter.class::cast).toList();
		List<XivPlayerCharacter> nonTargetPlayers = new ArrayList<>(state.partyList);
		nonTargetPlayers.removeAll(targetPlayers);
		nonTargetPlayers.sort(Comparator.comparing(player -> psJobPrio.indexOf(player.job)));

		log.info("[TOP_P5_Sigma_PS_AM] Bait players: {}", nonTargetPlayers);

		List<int> nonTargetGroups = nonTargetPlayers.stream().map(player -> headmarkerMap.get(player)).toList()
		List<XivPlayerCharacter> partyOrder = new ArrayList<>(state.partyList);
		partyOrder.sort(Comparator.comparing(player -> psJobPrio.indexOf(player.job)));
		List<int> markerOrder = [393, 395, 396, 394]
		
		partyOrder.removeAll(nonTargetPlayers)
		partyOrder.add(0, nonTargetPlayers.get(0))
		partyOrder.add(nonTargetPlayers.get(1))
		
		markerOrder.removeAll(nonTargetGroups)
		markerOrder.add(0, nonTargetGroups.get(0))
		markerOrder.add(nonTargetGroups.get(1))

		// Java sort guaranteed to be stable
		List<XivPlayerCharacter> psPlayerOrder = headmarkerMap.entrySet().stream()
			.sorted(Comparator.comparing(e -> partyOrder.indexOf(e.getKey())))
			.sorted(Comparator.comparing(e -> markerOrder.indexOf(e.getValue())))
			.map(e -> e.getKey()).toList()

		log.info("[TOP_P5_Sigma_PS_AM] PS order: {}", psPlayerOrder);

		s.accept(new SpecificAutoMarkRequest(psPlayerOrder.get(0), MarkerSign.ATTACK1));  // bait 1
		s.accept(new SpecificAutoMarkRequest(psPlayerOrder.get(1), MarkerSign.SQUARE));
		s.accept(new SpecificAutoMarkRequest(psPlayerOrder.get(2), MarkerSign.ATTACK2));
		s.accept(new SpecificAutoMarkRequest(psPlayerOrder.get(3), MarkerSign.BIND3));
		s.accept(new SpecificAutoMarkRequest(psPlayerOrder.get(4), MarkerSign.ATTACK3));
		s.accept(new SpecificAutoMarkRequest(psPlayerOrder.get(5), MarkerSign.BIND2));    
		s.accept(new SpecificAutoMarkRequest(psPlayerOrder.get(6), MarkerSign.ATTACK4));
		s.accept(new SpecificAutoMarkRequest(psPlayerOrder.get(7), MarkerSign.BIND1));    // bait 2
		
		// discharge
		s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B2E));
		s.accept(new ClearAutoMarkRequest());

		// near 0xD72, dist 0xD73, dynamis 0xD74
		s.waitMs(100);  // wait for clear marker
		List<XivPlayerCharacter> party = new ArrayList<>(state.partyList);
		XivPlayerCharacter near = (XivPlayerCharacter) statusEffectRepository.findBuffById(0xD72).getTarget();
		XivPlayerCharacter dist = (XivPlayerCharacter) statusEffectRepository.findBuffById(0xD73).getTarget();
		log.info("[TOP_P5_Sigma_HW_AM] HW near: {}", near);
		log.info("[TOP_P5_Sigma_HW_AM] HW dist: {}", dist);
		
		party.remove(near);
		party.remove(dist);
		party.sort(Comparator.comparing(player -> psJobPrio.indexOf(player.job)));
		party.sort(Comparator.comparing(player -> -statusEffectRepository.buffStacksOnTarget(player, 0xD74)));
		log.info("[TOP_P5_Sigma_HW_AM] Party: {}", party);
		
		s.accept(new SpecificAutoMarkRequest(near, MarkerSign.CIRCLE));          // HW near
		s.accept(new SpecificAutoMarkRequest(dist, MarkerSign.CROSS));           // HW dist
		s.accept(new SpecificAutoMarkRequest(party.get(0), MarkerSign.IGNORE1)); // laser baiter 1
		s.accept(new SpecificAutoMarkRequest(party.get(1), MarkerSign.IGNORE2)); // laser baiter 2
		s.accept(new SpecificAutoMarkRequest(party.get(2), MarkerSign.ATTACK1)); // dist baiter 1
		s.accept(new SpecificAutoMarkRequest(party.get(3), MarkerSign.ATTACK2)); // dist baiter 2
		s.accept(new SpecificAutoMarkRequest(party.get(4), MarkerSign.BIND1));   // near baiter 1
		s.accept(new SpecificAutoMarkRequest(party.get(5), MarkerSign.BIND2));   // near baiter 2

		s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0xD72));
		s.accept(new ClearAutoMarkRequest());
	}}
});