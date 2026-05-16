// ============================================================
// Equipo.java
// Estructura de datos para nodos de red (routers, switches, VMs)
// Autor: Raul Heredia (GitHub: RuloHeredia)
// ============================================================

public class Equipo {

    private String nombre;
    private String descripcion;
    private String ip;
    private String mac;
    private int    vlanAsignada;
    private String tipo;     // Físico, VM, Contenedor
    private String estado;   // Activo, Inactivo, Mantenimiento
    private String usuario;
    private String contrasena;

    public Equipo(String nombre, String descripcion, String ip, String mac, 
            int vlanAsignada, String tipo, String estado, 
            String usuario, String contrasena) {
        this.nombre       = nombre;
        this.descripcion  = descripcion;
        this.ip           = ip;
        this.mac          = mac;
        this.vlanAsignada = vlanAsignada;
        this.tipo         = tipo;
        this.estado       = estado;
        this.usuario      = usuario;
        this.contrasena   = contrasena;
    }

    public String getNombre()       { return nombre; }
    public String getDescripcion()  { return descripcion; }
    public String getIp()           { return ip; }
    public String getMac()          { return mac; }
    public int    getVlanAsignada() { return vlanAsignada; }
    public String getTipo()         { return tipo; }
    public String getEstado()       { return estado; }
    public String getUsuario()      { return usuario; }
    public String getContrasena()   { return contrasena; }

    public void setNombre(String nombre)             { this.nombre = nombre; }
    public void setDescripcion(String descripcion)   { this.descripcion = descripcion; }
    public void setIp(String ip)                     { this.ip = ip; }
    public void setMac(String mac)                   { this.mac = mac; }
    public void setVlanAsignada(int vlanAsignada)    { this.vlanAsignada = vlanAsignada; }
    public void setTipo(String tipo)                 { this.tipo = tipo; }
    public void setEstado(String estado)             { this.estado = estado; }
    public void setUsuario(String usuario)           { this.usuario = usuario; }
    public void setContrasena(String contrasena)     { this.contrasena = contrasena; }
}