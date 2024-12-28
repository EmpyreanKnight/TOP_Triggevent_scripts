// PROP: scriptName=TOP_P2_PS
// PROP: strict=false
// PROP: startup=true

import java.time.Duration;
import java.util.stream.Collectors;

generalJobPrio = [
	Job.WHM, Job.AST, 
	Job.PLD, Job.WAR, Job.GNB, Job.DRK, 
	Job.NIN, Job.SAM, Job.MNK, Job.RPR, Job.DRG,
	Job.BRD, Job.MCH, Job.DNC, 
	Job.BLM, Job.RDM, Job.SMN, 
	Job.SCH, Job.SGE
]

class PsMarker implements Comparable<PsMarker> {
	static PsMarker GROUP1_CIRCLE   = new PsMarker("Group 1 Circle", 1, 1);
	static PsMarker GROUP1_CROSS    = new PsMarker("Group 1 Cross", 1, 2);
	static PsMarker GROUP1_TRIANGLE = new PsMarker("Group 1 Triangle", 1, 3);
	static PsMarker GROUP1_SQUARE   = new PsMarker("Group 1 Square", 1, 4);
	static PsMarker GROUP2_CIRCLE   = new PsMarker("Group 2 Circle", 2, 1);
	static PsMarker GROUP2_CROSS    = new PsMarker("Group 2 Cross", 2, 2);
	static PsMarker GROUP2_TRIANGLE = new PsMarker("Group 2 Triangle", 2, 3);
	static PsMarker GROUP2_SQUARE   = new PsMarker("Group 2 Square", 2, 4);
	static List<PsMarker> PsMarkerList = [
		//GROUP1_CIRCLE, GROUP1_CROSS, GROUP1_TRIANGLE, GROUP1_SQUARE,
		GROUP1_CIRCLE, GROUP1_TRIANGLE, GROUP1_SQUARE, GROUP1_CROSS,
		//GROUP2_CIRCLE, GROUP2_CROSS, GROUP2_TRIANGLE, GROUP2_SQUARE
		GROUP2_CIRCLE, GROUP2_TRIANGLE, GROUP2_SQUARE, GROUP2_CROSS
	];

	private final String friendlyName;
	private final int group;
	private final int order;
	private PsMarker(String friendlyName, int group, int order) {
		this.friendlyName = friendlyName;
		this.group = group;
		this.order = order;
	}

	public int getGroup() {
		return this.@group;
	}

	public int getOrder() {
		return this.@order;	
	}
	
	public int getPrio() {
		return (this.@group - 1) * 4 + (this.@order - 1);
	}

	public String toString() {
        return friendlyName;
    	}

	public static PsMarker getMarker(int offset, int group) {
		return PsMarkerList.get((group - 1) * 4 + (offset - 393));
	}
	
	public PsMarker getCounterpart() {
		return PsMarkerList.get((this.getPrio() + 4) % 8);
	}
	
	@Override
	public int compareTo(PsMarker another) {
	   return this.getPrio() - another.getPrio();
	}
}

psMarkSettingMid = Map.of(
		PsMarker.GROUP1_CIRCLE, MarkerSign.ATTACK1,
		PsMarker.GROUP1_CROSS, MarkerSign.ATTACK2,
		PsMarker.GROUP1_TRIANGLE, MarkerSign.ATTACK3,
		PsMarker.GROUP1_SQUARE, MarkerSign.ATTACK4,
		PsMarker.GROUP2_CIRCLE, MarkerSign.BIND1,
		PsMarker.GROUP2_CROSS, MarkerSign.BIND2,
		PsMarker.GROUP2_TRIANGLE, MarkerSign.BIND3,
		PsMarker.GROUP2_SQUARE, MarkerSign.SQUARE
);

psMarkSettingFar = Map.of(
		PsMarker.GROUP1_CIRCLE, MarkerSign.ATTACK1,
		PsMarker.GROUP1_CROSS, MarkerSign.ATTACK2,
		PsMarker.GROUP1_TRIANGLE, MarkerSign.ATTACK3,
		PsMarker.GROUP1_SQUARE, MarkerSign.ATTACK4,
		PsMarker.GROUP2_CIRCLE, MarkerSign.SQUARE,
		PsMarker.GROUP2_CROSS, MarkerSign.BIND3,
		PsMarker.GROUP2_TRIANGLE, MarkerSign.BIND2,
		PsMarker.GROUP2_SQUARE, MarkerSign.BIND1
);

//List<PsMarker> a = [PsMarker.decodeOffset(396), PsMarker.decodeOffset(393), PsMarker.decodeOffset(395), PsMarker.decodeOffset(394)];
//log.error("${a}")
//log.error("${a.sort().reverse()}")


def TOP_P2_PS_AM(s) {
	s.accept(new ClearAutoMarkRequest());
	
	// capture 8 PS markers
	List<HeadMarkerEvent> headMarkers = s.waitEventsQuickSuccession(8, HeadMarkerEvent.class, hm -> hm.getTarget().isPc(), Duration.ofSeconds(1));
	log.info("[TOP_P2_PS_AM] PS headmarkers: {}", headMarkers);

	Map<int, List<HeadMarkerEvent>> offsetGroups = headMarkers.stream()
		.sorted(Comparator.comparing(hm -> generalJobPrio.indexOf(hm.getTarget().job)))
		.collect(Collectors.groupingBy(hm -> hm.getMarkerOffset()));
	log.info("[TOP_P2_PS_AM] PS offset groups: {}", offsetGroups);
	
	Map<XivPlayerCharacter, PsMarker> headmarkerMap = new HashMap<XivPlayerCharacter, PsMarker>();
	
	offsetGroups.forEach((offset, headmarkerList) -> {
		log.info("[TOP_P2_PS_AM] now processing offset: {}", offset);
		headmarkerMap.put(headmarkerList.get(0).getTarget(), PsMarker.getMarker(offset, 1));
		headmarkerMap.put(headmarkerList.get(1).getTarget(), PsMarker.getMarker(offset, 2));
	});
	log.info("[TOP_P2_PS_AM] PS headmarkers map: {}", headmarkerMap);
	
	boolean mid = statusEffectRepository.getBuffs().stream().filter(ba -> ba.buffIdMatches(0xD63, 0xD64)).anyMatch(ba -> ba.buffIdMatches(0xD63));
	//.findFirst().map(ba -> ba.buffIdMatches(0xD63)).stream().findFirst().orElse(false);

	log.info("[TOP_P2_PS_AM] mid? ${mid}");
	eventMaster.pushEvent(new TelestoGameCommand("/p ${mid ? "Mid <se.1>" : "Far <se.8>"}"));

	// PS AM
	headmarkerMap.forEach((player, assignment) -> {
		MarkerSign marker = (mid ? psMarkSettingMid.get(assignment) : psMarkSettingFar.get(assignment));
		if (marker != null) {
			log.info("[TOP_P2_PS_AM] PS Marker: {} on {}", marker, player);
			eventMaster.pushEvent(new SpecificAutoMarkRequest(player, marker));
			//s.accept(new SpecificAutoMarkRequest(player, marker));
		}
	});

	// capture 2 stack markers
	List<HeadMarkerEvent> stackMarkers = s.waitEvents(2, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == 77);
	
	List<XivPlayerCharacter> stackPlayers = stackMarkers.stream()
		.map(hme -> hme.getTarget())
		.map(XivPlayerCharacter.class::cast)
		.sorted(Comparator.comparing(player -> headmarkerMap.get(player).getOrder()))
		.toList();

	log.info("[TOP_P2_PS_AM] Stack players: {}", stackPlayers);

	// two stacks on the same side
	if (stackPlayers.size() >= 2 && headmarkerMap.get(stackPlayers.get(0)).getGroup() == headmarkerMap.get(stackPlayers.get(1)).getGroup()) {
		int stackGroupOrder = headmarkerMap.get(stackPlayers.get(1)).getOrder();
		if (!mid && headmarkerMap.get(stackPlayers.get(0)).getGroup() == 2) {
			// if far and stack players are on the rhs, the sorting order should be reversed
			stackGroupOrder = headmarkerMap.get(stackPlayers.get(0)).getOrder();
		}
		
		List<XivPlayerCharacter> swapPlayers = headmarkerMap.entrySet().stream()
			.filter(entry -> entry.getValue().getOrder() == stackGroupOrder)
			.sorted(Map.Entry.comparingByValue().reversed())
			.map(entry -> entry.getKey()).toList();

		log.info("[TOP_P2_PS_AM] Swap players: {}", swapPlayers);
		eventMaster.pushEvent(new TelestoGameCommand("/p Swap! <se.1>"));

		eventMaster.pushEvent(new SpecificAutoMarkRequest(swapPlayers.get(0), MarkerSign.IGNORE1));
		eventMaster.pushEvent(new SpecificAutoMarkRequest(swapPlayers.get(1), MarkerSign.IGNORE2));
	}
	
	// spotlight
	s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B30));
	s.accept(new ClearAutoMarkRequest());
}


groovyTriggers.add({
	named "TOP_P2_PS_AM"
	when { AbilityCastStart acs -> acs.abilityIdMatches(0x7B3F) }  // triggered by "Party Synergy" cast
	sequence { e1, s -> {
		TOP_P2_PS_AM(s);
	}}
});