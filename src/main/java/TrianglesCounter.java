import org.apache.giraph.Algorithm;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.graph.BasicComputation;
import org.apache.giraph.graph.Vertex;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.pig.data.DefaultTuple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Algorithm(
		name = "Triangles Count"
)
public class TrianglesCounter extends BasicComputation<
		IntWritable, IntWritable,
		NullWritable, DefaultTuple> {

	/**
	 * superstep 0:
	 * Do każdego sasiada o mniejszym indeksie wysylam wszystkie swoje krawedzie.
	 * Wysylamy tylko do sasiadow o mnieszym indeksie aby kazdy trojkat policzyc tylko raz.
	 * superstep 1:
	 * Sprawdzam czy dostalem 2 razy ta sama krawedz - jesli tak, to jest to krawedz
	 * domykajaca trojkat z wierzcholkami, z ktorych te krawedz dostalem.
	 */
	@Override
	public void compute(
			Vertex<IntWritable, IntWritable, NullWritable> vertex,
			Iterable<DefaultTuple> messages) {
		if (getSuperstep() == 0) {
			// collect and order edges
			List<Integer> lowerNeighbours = new ArrayList<Integer>();

			for (Edge<IntWritable, NullWritable> edge : vertex.getEdges()) {
				System.out.println("Neighbour of " + vertex.getId() + ": " + edge.getTargetVertexId().get());
				if (edge.getTargetVertexId().get() < vertex.getId().get()) {
					// wkladac integery nie intWritable, bo sie zle rzeczy dzialy!
					lowerNeighbours.add(edge.getTargetVertexId().get());
				}
			}
			System.out.println(vertex.getId().get() + ": lower neighbours: " + lowerNeighbours.toString());
			for (Edge<IntWritable, NullWritable> edge : vertex.getEdges()) {
				int a = edge.getTargetVertexId().get();
				int b = vertex.getId().get();
				DefaultTuple tupleEdge = new DefaultTuple();
				if (a > b) {
					tupleEdge.append(b);
					tupleEdge.append(a);
				} else {
					tupleEdge.append(a);
					tupleEdge.append(b);
				}
				for (Integer n : lowerNeighbours) {
					System.out.println("Send edge: " + tupleEdge + " from: " + vertex.getId() + " to: " + n);
					sendMessage(new IntWritable(n), tupleEdge);
				}
			}

		} else {
			int res = 0;
			Set<DefaultTuple> edges = new HashSet<DefaultTuple>();
			for (DefaultTuple message : messages) {
				System.out.println(vertex.getId().get() + ": " + message.toString());
				if (!edges.add(message)) {
					System.out.println(vertex.getId().get() + ": res++");
					// already added
					res++;
				}
			}
			vertex.setValue(new IntWritable(res));
		}
		vertex.voteToHalt();
	}
}
