package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Graph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private PlayerConfiguration mrX;
	private PlayerConfiguration firstDetective;
	private PlayerConfiguration restOfTheDetectives;
	public List<ScotlandYardPlayer> players = new ArrayList<>();

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {
		// TODO
		//ensure rounds and graph cannot be null
		this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);
		//ensure that rounds is not empty
		if (rounds.isEmpty()) {
			throw new IllegalArgumentException("Empty rounds");
		}
		//ensure that graph is not empty
		if(graph.isEmpty()) {
			throw new IllegalArgumentException("Empty graph");
		}
		//ensure that MrX is black
		if (mrX.colour != BLACK) { // or mr.colour.isDetective()
			throw new IllegalArgumentException("MrX should be Black");
		}
		//put players into temporary list so that we can perform checks
		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		//configurations.add(0, mrX);
		//configurations.add(0, firstDetective);
		for (PlayerConfiguration configuration : restOfTheDetectives)
			configurations.add(requireNonNull(configuration));
		configurations.add(0, firstDetective);
		configurations.add(0, mrX);
		//iterate over list to check validity
		Set<Integer> set1 = new HashSet<>();
		Set<Colour> set2 = new HashSet<>();
		//List<ScotlandYardPlayer> interim = new ArrayList<>();
		for (PlayerConfiguration configuration : configurations) {
			//check for location duplication
			if (set1.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");
			set1.add(configuration.location);
			//check for colour duplication
			if(set2.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate colour");

			//check all ticket types exist
			if(!(configuration.tickets.containsKey(Ticket.TAXI) && configuration.tickets.containsKey(Ticket.BUS) && configuration.tickets.containsKey(Ticket.UNDERGROUND)
					&& configuration.tickets.containsKey(SECRET) && configuration.tickets.containsKey(DOUBLE)))
				throw new IllegalArgumentException("Missing ticket type");

			//check for detectives with secret or double tickets
			if(((configuration.tickets.get(SECRET)>0) || (configuration.tickets.get(DOUBLE)>0)) && (configuration.colour != BLACK))
				throw new IllegalArgumentException("Detective holds secret or double card");

			//create list of players
			ScotlandYardPlayer player = new ScotlandYardPlayer(configuration.player, configuration.colour,
					configuration.location, configuration.tickets);
			//if(player == null)
			//	throw new IllegalArgumentException("player is null");
			players.add(player);
		}
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		// TODO
		if(isGameOver() == true) {
			throw new IllegalStateException("game is already over");
		}
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		// TODO
		List<Colour> playerColour = new ArrayList<>();
		for(ScotlandYardPlayer eg : players) {
			//if(eg.colour() != null) {
				playerColour.add(eg.colour());
			//}
		}
		return Collections.unmodifiableList(playerColour);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public boolean isGameOver() {
		// TODO
		if(rounds.size() > 1) {return false;}
		return true;
	}

	@Override
	public Colour getCurrentPlayer() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public int getCurrentRound() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<Integer, Transport>(graph);
	}

}
