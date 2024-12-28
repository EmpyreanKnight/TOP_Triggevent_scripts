// PROP: scriptName=TOP_P5_Omega_HW
// PROP: strict=false
// PROP: startup=false

generalJobPrio = [
	Job.WHM, Job.AST, 
	Job.BRD, Job.MCH, Job.DNC, 
	Job.BLM, Job.RDM, Job.SMN, 
	Job.SCH, Job.SGE,
	Job.PLD, Job.WAR, Job.GNB, Job.DRK, 
	Job.NIN, Job.SAM, Job.MNK, Job.RPR, Job.DRG
]


groovyTriggers.add({
	named "TOP_P5_Omega_HW_AM"
	// Run: Dynamis (Omega)
	when { AbilityUsedEvent aue -> aue.abilityIdMatches(0x8015) }
	sequence { e1, s -> {
		// D72 (3442) - Near (32s short, 50s long)
		// D73 (3443) - Dist (32s short, 50s long)
		// D74 (3444) - Dynamis
		// BBC (3004) - First in Line
		// BBD (3005) - Second in Line
		s.waitEvents(2, BuffApplied.class, ba -> ba.buffIdMatches(0xBBD));
		s.waitMs(100);
		
		XivPlayerCharacter shortNear = (XivPlayerCharacter) statusEffectRepository.findBuff(ba -> ba.buffIdMatches(0xD72) && ba.getInitialDuration().toSeconds() < 40).getTarget();
		XivPlayerCharacter shortDist = (XivPlayerCharacter) statusEffectRepository.findBuff(ba -> ba.buffIdMatches(0xD73) && ba.getInitialDuration().toSeconds() < 40).getTarget();
		XivPlayerCharacter longNear = (XivPlayerCharacter) statusEffectRepository.findBuff(ba -> ba.buffIdMatches(0xD72) && ba.getInitialDuration().toSeconds() > 40).getTarget();
		XivPlayerCharacter longDist = (XivPlayerCharacter) statusEffectRepository.findBuff(ba -> ba.buffIdMatches(0xD73) && ba.getInitialDuration().toSeconds() > 40).getTarget();
		
		List<XivPlayerCharacter> partyList = new ArrayList<>(state.partyList);
		party.sort(Comparator.comparing(player -> generalJobPrio.indexOf(player.job)));

		List<XivPlayerCharacter> playersToMark = partyList.stream()
			.sorted(Comparator.comparing(member -> statusEffectRepository.getBuffs().buffStacksOnTarget(member, 3444)))
			.filter(member -> {
				if (statusEffectRepository.getBuffs().isStatusOnTarget(member, 0xBBC)) {
					return false;
				}
				else if (statusEffectRepository.getBuffs().buffStacksOnTarget(member, 0xD74) == 1) {
					return true;
				}
				else if (statusEffectRepository.getBuffs().buffStacksOnTarget(member, 0xD74) == 2 && 
				         !statusEffectRepository.getBuffs().isStatusOnTarget(member, 0xBBD)) {
					return true;
				}
				return false;
			})
			.limit(4)
			.toList();
			
		List<XivPlayerCharacter> leftovers = new ArrayList<>(partyList);
		leftovers.remove(shortNear));
		leftovers.remove(shortDist);
		leftovers.removeAll(playersToMark);
		
		s.accept(new SpecificAutoMarkRequest(shortNear, MarkerSign.CIRCLE));             // HW near
		s.accept(new SpecificAutoMarkRequest(shortDist, MarkerSign.CROSS));              // HW dist
		s.accept(new SpecificAutoMarkRequest(leftovers.get(0), MarkerSign.IGNORE1));     // monitor baiter 1
		s.accept(new SpecificAutoMarkRequest(leftovers.get(1), MarkerSign.IGNORE2));     // monitor baiter 2
		s.accept(new SpecificAutoMarkRequest(playersToMark.get(0), MarkerSign.ATTACK1)); // dist baiter 1
		s.accept(new SpecificAutoMarkRequest(playersToMark.get(1), MarkerSign.ATTACK2)); // dist baiter 2
		s.accept(new SpecificAutoMarkRequest(playersToMark.get(2), MarkerSign.BIND1));   // near baiter 1
		s.accept(new SpecificAutoMarkRequest(playersToMark.get(3), MarkerSign.BIND2));   // near baiter 2

		// alignment
		AbilityCastStart diffuseWaveCannon = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(31643, 31644));
		AbilityCastStart oversampledWaveCannon = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(31638, 31639));
		s.accept(new ClearAutoMarkRequest());

		// Wait for any of these, since at that point the mechanic is basically locked in
		// 7B89 - near initial
		// 8110 - dist initial
		// 7B8A - near followup
		// 8111 - dist followup
		// 7B6D - Oversampled wave cannon
		// Try to make this resilient even if something goes very wrong
		s.waitEventsUntil(2, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B8A) && aue.isFirstTarget(),
				AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7E76));
		s.waitMs(500);

		List<XivPlayerCharacter> partyList = new ArrayList<>(state.partyList);
		party.sort(Comparator.comparing(player -> generalJobPrio.indexOf(player.job)));
		List<XivPlayerCharacter> twoStackPlayers = partyList.stream()
			.filter(member -> statusEffectRepository.getBuffs().buffStacksOnTarget(member, 0xD74) == 2
			              && !statusEffectRepository.getBuffs().isStatusOnTarget(member, 0xBBC)
			              && !statusEffectRepository.getBuffs().isStatusOnTarget(member, 0xBBD))
			.limit(4)
			.toList();
		List<XivPlayerCharacter> threeStackPlayers = partyList.stream()
			.filter(member -> statusEffectRepository.getBuffs().buffStacksOnTarget(member, 0xD74) == 3
			              && !statusEffectRepository.getBuffs().isStatusOnTarget(member, 0xBBC)
			              && !statusEffectRepository.getBuffs().isStatusOnTarget(member, 0xBBD))
			.limit(2)
			.toList();

		s.accept(new SpecificAutoMarkRequest(longNear, MarkerSign.CIRCLE));                  // HW near
		s.accept(new SpecificAutoMarkRequest(longDist, MarkerSign.CROSS));                   // HW dist
		s.accept(new SpecificAutoMarkRequest(threeStackPlayers.get(0), MarkerSign.IGNORE1)); // impact baiter 1
		s.accept(new SpecificAutoMarkRequest(threeStackPlayers.get(1), MarkerSign.IGNORE2)); // impact baiter 2
		s.accept(new SpecificAutoMarkRequest(twoStackPlayers.get(0), MarkerSign.ATTACK1));   // dist baiter 1
		s.accept(new SpecificAutoMarkRequest(twoStackPlayers.get(1), MarkerSign.ATTACK2));   // dist baiter 2
		s.accept(new SpecificAutoMarkRequest(twoStackPlayers.get(2), MarkerSign.BIND1));     // near baiter 1
		s.accept(new SpecificAutoMarkRequest(twoStackPlayers.get(3), MarkerSign.BIND2));     // near baiter 2
		
		s.waitMs(15_000);
		//s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B48));
		s.accept(new ClearAutoMarkRequest());
	}}
});