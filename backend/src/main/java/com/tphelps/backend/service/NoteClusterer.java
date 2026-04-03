package com.tphelps.backend.service;

import com.tphelps.backend.service.pojos.AdjacentNote;
import com.tphelps.backend.service.pojos.NoteEdges;

import java.util.*;

public class NoteClusterer {

    /**
     * Method to cluster our edges list from the db
     *
     * This method will create an adjacency list from our edges stored in note_links, from
     * there it will then build our clusters through related notes to send to the front end
     *
     * Clusters can share notes, they are not unique to a cluster
     *
     * @param edgesList - edge list from note_links
     * @return - a clustered map of (cluster, set of note titles)
     */
    public static Map<String, Set<String>> clusterEdges(List<NoteEdges> edgesList) {
        Map<Integer, List<AdjacentNote>> adjacencyMap = createAdjacencyMap(edgesList);

        return clusterAdjacencyTitles(edgesList, adjacencyMap);
    }

    /**
     * Builds adjacency list (map) with (Id -> List of adjacentNotes)
     * @param edgesList
     * @return
     */
    private static Map<Integer, List<AdjacentNote>> createAdjacencyMap(List<NoteEdges> edgesList) {
        Map<Integer, List<AdjacentNote>> adjacencyMap = new HashMap<>();

        for(NoteEdges edge : edgesList) {
            adjacencyMap.computeIfAbsent(edge.from_note_id(), k -> new ArrayList<>())
                    .add(new AdjacentNote(
                            edge.to_note_id(), // if i have an empty to_note_id, then i have an empty similarity score
                            edge.similarity_score(),
                            edge.title()));
        }
        return adjacencyMap;
    }

    /**
     * Cluster adjacent titles together
     * @param edgesList
     * @param adjacencyMap
     * @return
     */
    private static Map<String, Set<String>> clusterAdjacencyTitles(
            List<NoteEdges> edgesList,
            Map<Integer, List<AdjacentNote>> adjacencyMap) {

        HashMap<String, Set<String>> clusteredEdges = new HashMap<>();

        for (int i = 0; i < edgesList.size(); i++) {
            NoteEdges edges = edgesList.get(i);

            int from = edges.from_note_id();
            Integer to = edges.to_note_id();
            String formattedClusterTitle = String.format("Cluster_%c", (char)('A' + i));

            clusteredEdges.computeIfAbsent(formattedClusterTitle, k -> new HashSet<>())
                    .add(edges.title());

            if (to != null) {
                for (AdjacentNote adjacentNote : adjacencyMap.get(from)) {

                    String title = adjacencyMap.get(adjacentNote.to_note_id()).get(0).title();
                    clusteredEdges.get(formattedClusterTitle).add(title);

                }
            }
        }
        return clusteredEdges;
    }

}
