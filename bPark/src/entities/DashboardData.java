package entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A data transfer object (DTO) that holds key metrics 
 * to be displayed on the manager's dashboard UI.
 * 
 * This class is serializable and typically sent from server to client.
 */
public class DashboardData implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Total number of parking spots in the system */
	private int totalSpots;

	/** Number of currently occupied parking spots */
	private int occupied;

	/** Number of currently available parking spots */
	private int available;

	/** Number of active (ongoing) reservations */
	private int activeReservations;

	/** 
	 * A map representing the number of reservations by type.
	 * Keys might include "Available", "Reserved", "Immediate", etc.
	 */
	private Map<String, Integer> reservationTypes = new HashMap<>();

	// ===== Getters and Setters =====

	/** @return total number of parking spots */
	public int getTotalSpots() {
		return totalSpots;
	}

	/** @param totalSpots total number of parking spots to set */
	public void setTotalSpots(int totalSpots) {
		this.totalSpots = totalSpots;
	}

	/** @return number of currently occupied spots */
	public int getOccupied() {
		return occupied;
	}

	/** @param occupied number of occupied spots to set */
	public void setOccupied(int occupied) {
		this.occupied = occupied;
	}

	/** @return number of currently available spots */
	public int getAvailable() {
		return available;
	}

	/** @param available number of available spots to set */
	public void setAvailable(int available) {
		this.available = available;
	}

	/** @return number of active reservations */
	public int getActiveReservations() {
		return activeReservations;
	}

	/** @param activeReservations number of active reservations to set */
	public void setActiveReservations(int activeReservations) {
		this.activeReservations = activeReservations;
	}

	/** 
	 * @return a map of reservation types and their corresponding counts 
	 */
	public Map<String, Integer> getReservationTypes() {
		return reservationTypes;
	}

	/** 
	 * @param reservationTypes map of reservation types and their counts to set 
	 */
	public void setReservationTypes(Map<String, Integer> reservationTypes) {
		this.reservationTypes = reservationTypes;
	}
}
