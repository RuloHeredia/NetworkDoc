// ============================================================
// Red.java
// Modelo representativo de la subred y agrupador logico de nodos
// Autor: Raul Heredia (GitHub: RuloHeredia)
// ============================================================

import java.util.ArrayList;
import java.util.List;

public class Red {

    private String       ipRed;
    private int          slash;
    private String       broadcast;
    private int          vlanRed;
    private List<Equipo> equipos;

    public Red(String ipRed, int slash, String broadcast, int vlanRed) {
        this.ipRed     = ipRed;
        this.slash     = slash;
        this.broadcast = broadcast;
        this.vlanRed   = vlanRed;
        this.equipos   = new ArrayList<>();
    }

    public String       getIpRed()     { return ipRed; }
    public int          getSlash()     { return slash; }
    public String       getBroadcast() { return broadcast; }
    public int          getVlanRed()   { return vlanRed; }
    public List<Equipo> getEquipos()   { return equipos; }

    public void setIpRed(String ipRed)         { this.ipRed = ipRed; }
    public void setSlash(int slash)            { this.slash = slash; }
    public void setBroadcast(String broadcast) { this.broadcast = broadcast; }
    public void setVlanRed(int vlanRed)        { this.vlanRed = vlanRed; }

    public void agregarEquipo(Equipo equipo) {
        this.equipos.add(equipo);
    }
    
    public void eliminarEquipo(Equipo equipo) {
        this.equipos.remove(equipo);
    }

    @Override
    public String toString() {
        return ipRed + "/" + slash + "  [VLAN " + vlanRed + "]";
    }
}