public class tile {

    private int tileType;
    private entity occupant;

    public int getTileType() {
        return tileType;
    }

    public boolean isEmpty() {
        return (tileType == map.EMPTY_TILE_CODE && occupant == null);
    }

    public void setTileType(int tileType) {
        this.tileType = tileType;
    }

    public entity getOccupant() {
        return occupant;
    }

    public void setOccupant(entity occupant) {
        this.occupant = occupant;
    }

    public tile(int tileType) {
        this.tileType = tileType;
        this.occupant = null;
    }
}
