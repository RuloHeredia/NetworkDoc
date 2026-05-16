// ============================================================
// NetworkDoc.java
// Sistema de Documentacion de Redes - Interfaz Grafica y Motor Local
// Incluye cifrado AES-128 para credenciales.
// Autor: Raul Heredia (GitHub: RuloHeredia)
// ============================================================

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Base64;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class NetworkDoc extends JFrame {

    private static final Color BG_DARK      = new Color(0x0F1923);
    private static final Color BG_PANEL     = new Color(0x13202D);
    private static final Color BG_CARD      = new Color(0x1E3448);
    private static final Color BG_INPUT     = new Color(0x11202F);
    private static final Color ACCENT_CYAN  = new Color(0x00C8E0);
    private static final Color TEXT_WHITE   = new Color(0xEEF4FF);
    private static final Color TEXT_MUTED   = new Color(0x7A9AB8);
    private static final Color TEXT_LABEL   = new Color(0xA8C4DC);
    private static final Color BORDER_COLOR = new Color(0x2A4A66);
    private static final Color ROW_ALT      = new Color(0x162233);
    private static final Color RED_ERROR    = new Color(0xE05555);
    private static final Color GREEN_OK     = new Color(0x3DD68C);

    private static final Font FONT_TITLE = new Font("Consolas", Font.BOLD,  22);
    private static final Font FONT_BODY  = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FONT_SMALL = new Font("Consolas", Font.PLAIN, 11);
    private static final Font FONT_LABEL = new Font("Consolas", Font.BOLD,  11);
    private static final Font FONT_BTN   = new Font("Consolas", Font.BOLD,  12);
    private static final Font FONT_MONO  = new Font("Consolas", Font.PLAIN, 12);

    private static final String ARCHIVO_DATOS = "networkdoc_data.json";
    
    // Llave secreta para cifrado AES (Debe ser de 16 caracteres para 128 bits)
    private static final String AES_KEY = "N3tw0rkD0cK3y123";

    private List<Red> listaRedes = new ArrayList<>();
    private JPanel contentPanel;
    private JLabel statusLabel;
    private String filtroActual = "";

    public NetworkDoc() {
        configurarVentana();
        construirUI();
        cargarDatos();
        mostrarVistaPrincipal();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                guardarDatos();
                System.exit(0);
            }
        });
    }

    private void configurarVentana() {
        setTitle("NetworkDoc v2.0 -- DCIM & IPAM (AES Encrypted)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());
    }

    private void construirUI() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_PANEL);
        topBar.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT_CYAN));
        topBar.setPreferredSize(new Dimension(0, 54));

        JLabel logoLabel = new JLabel("  * NetworkDoc");
        logoLabel.setFont(FONT_TITLE);
        logoLabel.setForeground(ACCENT_CYAN);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
        topBar.add(logoLabel, BorderLayout.WEST);

        JLabel versionLabel = new JLabel("v2.0   ");
        versionLabel.setFont(FONT_SMALL);
        versionLabel.setForeground(TEXT_MUTED);
        topBar.add(versionLabel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        add(crearSidebar(), BorderLayout.WEST);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BG_DARK);
        add(contentPanel, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 5));
        statusBar.setBackground(new Color(0x0B1520));
        statusBar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_COLOR));

        JLabel arrow = new JLabel(">");
        arrow.setFont(FONT_SMALL);
        arrow.setForeground(ACCENT_CYAN);
        statusBar.add(arrow);

        statusLabel = new JLabel("Inicializacion completada. Motor Crypto activo.");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_MUTED);
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel crearSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_PANEL);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 2, BORDER_COLOR));
        sidebar.setPreferredSize(new Dimension(195, 0));

        sidebar.add(Box.createVerticalStrut(22));
        sidebar.add(crearBotonSidebar("  [H]  Inicio ",           () -> mostrarVistaPrincipal()));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(crearBotonSidebar("  [+]  Agregar Red ",      () -> mostrarFormularioRed(null)));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(crearBotonSidebar("  [+]  Agregar Equipo",    () -> mostrarFormularioEquipo(null, null)));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(crearBotonSidebar("  [=]  Topologia",         () -> { filtroActual = ""; mostrarVistaRedes(); }));
        sidebar.add(Box.createVerticalGlue());

        JLabel authorLabel = new JLabel("<html><center>Raul Heredia<br>GitHub: RuloHeredia</center></html>");
        authorLabel.setFont(new Font("Consolas", Font.PLAIN, 10));
        authorLabel.setForeground(new Color(0x3A5570));
        authorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        authorLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        sidebar.add(authorLabel);

        return sidebar;
    }

    private JButton crearBotonSidebar(String texto, Runnable accion) {
        JButton btn = new JButton(texto);
        btn.setFont(FONT_BTN);
        btn.setForeground(TEXT_LABEL);
        btn.setBackground(BG_PANEL);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(195, 38));
        btn.setPreferredSize(new Dimension(195, 38));
        btn.setHorizontalAlignment(SwingConstants.LEFT);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(BG_CARD); btn.setForeground(ACCENT_CYAN); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BG_PANEL); btn.setForeground(TEXT_LABEL); }
        });
        btn.addActionListener(e -> accion.run());
        return btn;
    }

    private void mostrarVistaPrincipal() {
        contentPanel.removeAll();
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));

        JLabel titulo = new JLabel("Dashboard de Infraestructura");
        titulo.setFont(new Font("Consolas", Font.BOLD, 20));
        titulo.setForeground(TEXT_WHITE);
        wrapper.add(titulo, BorderLayout.NORTH);

        int totalEquipos = listaRedes.stream().mapToInt(r -> r.getEquipos().size()).sum();
        long totalVlans  = listaRedes.stream().map(Red::getVlanRed).distinct().count();

        JPanel cards = new JPanel(new GridLayout(1, 3, 18, 0));
        cards.setBackground(BG_DARK);
        cards.setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 0));
        cards.add(crearTarjetaResumen("Subredes L3", String.valueOf(listaRedes.size()), ACCENT_CYAN));
        cards.add(crearTarjetaResumen("Equipos Registrados", String.valueOf(totalEquipos), GREEN_OK));
        cards.add(crearTarjetaResumen("VLANs Activas", String.valueOf(totalVlans), new Color(0xF4A636)));
        wrapper.add(cards, BorderLayout.CENTER);

        JPanel accesos = new JPanel(new GridLayout(1, 2, 18, 0));
        accesos.setBackground(BG_DARK);
        accesos.add(crearBotonAccion("+ Agregar Red", ACCENT_CYAN, () -> mostrarFormularioRed(null)));
        accesos.add(crearBotonAccion("+ Nuevo Equipo", new Color(0x3DD68C), () -> mostrarFormularioEquipo(null, null)));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG_DARK);
        JLabel hint = new JLabel("Acciones rapidas:");
        hint.setFont(FONT_LABEL);
        hint.setForeground(TEXT_MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        bottom.add(hint, BorderLayout.NORTH);
        bottom.add(accesos, BorderLayout.CENTER);
        wrapper.add(bottom, BorderLayout.SOUTH);

        contentPanel.add(wrapper, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
        setStatus("Panel principal cargado.");
    }

    private JPanel crearTarjetaResumen(String tituloTarjeta, String valor, Color colorAcento) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1), BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));
        JLabel valLabel = new JLabel(valor);
        valLabel.setFont(new Font("Consolas", Font.BOLD, 36));
        valLabel.setForeground(colorAcento);
        JLabel titLabel = new JLabel(tituloTarjeta);
        titLabel.setFont(FONT_LABEL);
        titLabel.setForeground(TEXT_MUTED);
        card.add(valLabel, BorderLayout.CENTER);
        card.add(titLabel, BorderLayout.SOUTH);
        return card;
    }

    private JButton crearBotonAccion(String texto, Color color, Runnable accion) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Consolas", Font.BOLD, 13));
        btn.setForeground(BG_DARK);
        btn.setBackground(color);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 42));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(color.brighter()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(color); }
        });
        btn.addActionListener(e -> accion.run());
        return btn;
    }

    private void mostrarFormularioRed(Red redAEditar) {
        contentPanel.removeAll();
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));

        String txtTitulo = (redAEditar == null) ? "Agregar Nueva Red" : "Editar Red " + redAEditar.getIpRed();
        JLabel titulo = new JLabel(txtTitulo);
        titulo.setFont(new Font("Consolas", Font.BOLD, 18));
        titulo.setForeground(ACCENT_CYAN);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        wrapper.add(titulo, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG_CARD);
        form.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1), BorderFactory.createEmptyBorder(28, 32, 28, 32)
        ));

        JTextField campoIP = crearCampoTexto();
        JTextField campoSlash = crearCampoTexto();
        JTextField campoVlan = crearCampoTexto();

        if (redAEditar != null) {
            campoIP.setText(redAEditar.getIpRed());
            campoSlash.setText(String.valueOf(redAEditar.getSlash()));
            campoVlan.setText(String.valueOf(redAEditar.getVlanRed()));
        }

        form.add(crearLabelForm("Dirección de red (ej: 192.168.1.0)"));
        form.add(campoIP);
        form.add(Box.createVerticalStrut(14));

        form.add(crearLabelForm("Prefijo / Slash (ej: 24)"));
        form.add(campoSlash);
        form.add(Box.createVerticalStrut(14));

        form.add(crearLabelForm("VLAN ID (Opcional, ej: 10)"));
        form.add(campoVlan);
        form.add(Box.createVerticalStrut(20));

        JLabel broadcastResult = new JLabel("  Broadcast: --");
        broadcastResult.setFont(FONT_MONO);
        broadcastResult.setForeground(GREEN_OK);
        broadcastResult.setOpaque(true);
        broadcastResult.setBackground(BG_INPUT);
        broadcastResult.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1), BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
        broadcastResult.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        form.add(broadcastResult);
        form.add(Box.createVerticalStrut(22));

        FocusAdapter calcularAlSalir = new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String ip = campoIP.getText().trim();
                String slashStr = campoSlash.getText().replace("/", "").trim();
                try {
                    int sl = Integer.parseInt(slashStr);
                    String bc = calcularBroadcast(ip, sl);
                    if (bc != null) {
                        broadcastResult.setText("  Broadcast: " + bc + "/" + sl);
                        broadcastResult.setForeground(GREEN_OK);
                    } else {
                        broadcastResult.setText("  Broadcast: IP no valida");
                        broadcastResult.setForeground(RED_ERROR);
                    }
                } catch (Exception ex) {
                    broadcastResult.setText("  Broadcast: Esperando datos...");
                    broadcastResult.setForeground(TEXT_MUTED);
                }
            }
        };
        campoIP.addFocusListener(calcularAlSalir);
        campoSlash.addFocusListener(calcularAlSalir);
        if (redAEditar != null) calcularAlSalir.focusLost(null);

        JButton btnGuardar = crearBotonAccion("Guardar Red", ACCENT_CYAN, () -> {
            String ip = campoIP.getText().trim();
            String slashStr = campoSlash.getText().replace("/", "").trim();
            String vlanStr = campoVlan.getText().trim();

            if (ip.isEmpty()) { mostrarError("IP requerida."); return; }
            int sl, vlan = 1;
            try { sl = Integer.parseInt(slashStr); } catch (Exception ex) { mostrarError("Slash invalido."); return; }
            try { if(!vlanStr.isEmpty()) vlan = Integer.parseInt(vlanStr); } catch (Exception ex) { mostrarError("VLAN debe ser numerica."); return; }

            String bc = calcularBroadcast(ip, sl);
            if (bc == null) { mostrarError("IP de red invalida."); return; }

            if (redAEditar == null) {
                listaRedes.add(new Red(ip, sl, bc, vlan));
            } else {
                redAEditar.setIpRed(ip);
                redAEditar.setSlash(sl);
                redAEditar.setBroadcast(bc);
                redAEditar.setVlanRed(vlan);
            }
            guardarDatos();
            mostrarVistaRedes();
        });

        JButton btnCancelar = crearBotonAccion("Cancelar", new Color(0x2A4A66), () -> mostrarVistaRedes());
        JPanel botones = new JPanel(new GridLayout(1, 2, 12, 0));
        botones.setBackground(BG_CARD);
        botones.add(btnGuardar);
        botones.add(btnCancelar);
        botones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        form.add(botones);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 0.6; gbc.weighty = 1.0;
        center.add(form, gbc);

        wrapper.add(center, BorderLayout.CENTER);
        contentPanel.add(wrapper, BorderLayout.CENTER);
        contentPanel.revalidate(); contentPanel.repaint();
    }

    private void mostrarFormularioEquipo(Red redPreseleccionada, Equipo equipoAEditar) {
        if (listaRedes.isEmpty()) { mostrarError("No hay redes. Agrega una primero."); return; }

        contentPanel.removeAll();
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));

        String txtTitulo = (equipoAEditar == null) ? "Agregar Nuevo Equipo" : "Editar Equipo " + equipoAEditar.getIp();
        JLabel titulo = new JLabel(txtTitulo);
        titulo.setFont(new Font("Consolas", Font.BOLD, 18));
        titulo.setForeground(GREEN_OK);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        wrapper.add(titulo, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG_CARD);
        form.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1), BorderFactory.createEmptyBorder(28, 32, 28, 32)
        ));

        JComboBox<Red> comboRed = new JComboBox<>();
        listaRedes.forEach(comboRed::addItem);
        if (redPreseleccionada != null) comboRed.setSelectedItem(redPreseleccionada);
        estilizarCombo(comboRed);
        
        if(equipoAEditar != null) comboRed.setEnabled(false);

        form.add(crearLabelForm("Segmento de Red asociado"));
        form.add(comboRed);
        form.add(Box.createVerticalStrut(14));

        JPanel grid = new JPanel(new GridLayout(8, 2, 12, 5));
        grid.setBackground(BG_CARD);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JTextField campoNombre = crearCampoTexto();
        campoNombre.setToolTipText("Ejemplo: SW-Core-01, SRV-WEB o Laptop-Gerencia");
        
        JTextField campoDesc = crearCampoTexto();
        campoDesc.setToolTipText("Breve descripcion del rol del equipo");
        
        JTextField campoIP = crearCampoTexto();
        campoIP.setToolTipText("Solo la IP. Ejemplo: 192.168.1.10");
        
        JTextField campoMac = crearCampoTexto();
        campoMac.setToolTipText("Ejemplo: 00:1A:2B:3C:4D:5E");

        JComboBox<String> comboTipo = new JComboBox<>(new String[]{"Físico", "Máquina Virtual", "Contenedor", "Switch/Router"});
        estilizarCombo(comboTipo);
        
        JComboBox<String> comboEstado = new JComboBox<>(new String[]{"Activo", "Inactivo", "Mantenimiento"});
        estilizarCombo(comboEstado);
        
        JTextField campoUsuario = crearCampoTexto();
        JPasswordField campoClave = new JPasswordField();
        estilizarCampo(campoClave);

        if (equipoAEditar != null) {
            campoNombre.setText(equipoAEditar.getNombre());
            campoDesc.setText(equipoAEditar.getDescripcion());
            campoIP.setText(equipoAEditar.getIp().split("/")[0]);
            campoMac.setText(equipoAEditar.getMac());
            comboTipo.setSelectedItem(equipoAEditar.getTipo());
            comboEstado.setSelectedItem(equipoAEditar.getEstado());
            campoUsuario.setText(equipoAEditar.getUsuario());
            campoClave.setText(equipoAEditar.getContrasena());
        }

        grid.add(crearLabelForm("Nombre del Dispositivo (Hostname)")); 
        grid.add(crearLabelForm("Descripción o Rol (ej: Switch Piso 1)"));
        grid.add(campoNombre); 
        grid.add(campoDesc);
        
        grid.add(crearLabelForm("Dirección IP (ej: 192.168.1.50)")); 
        grid.add(crearLabelForm("Dirección MAC (ej: AA:BB:CC:11:22:33)"));
        grid.add(campoIP); 
        grid.add(campoMac);
        
        grid.add(crearLabelForm("Tipo de Infraestructura")); 
        grid.add(crearLabelForm("Estado Actual"));
        grid.add(comboTipo); 
        grid.add(comboEstado);
        
        grid.add(crearLabelForm("Usuario de Acceso (ej: admin)")); 
        grid.add(crearLabelForm("Contraseña (Se guarda cifrada)"));
        grid.add(campoUsuario); 
        grid.add(campoClave);

        form.add(grid);
        form.add(Box.createVerticalStrut(22));

        JButton btnGuardar = crearBotonAccion("Guardar Configuración", GREEN_OK, () -> {
            Red redSel = (Red) comboRed.getSelectedItem();
            String nombre = campoNombre.getText().trim();
            String desc = campoDesc.getText().trim();
            String ipEq = campoIP.getText().trim();
            String mac = campoMac.getText().trim();
            String tipo = (String) comboTipo.getSelectedItem();
            String estado = (String) comboEstado.getSelectedItem();
            String usuario = campoUsuario.getText().trim();
            String clave = new String(campoClave.getPassword()).trim();

            if (nombre.isEmpty() || ipEq.isEmpty()) { mostrarError("El Nombre y la IP son campos obligatorios."); return; }
            if (mac.isEmpty()) mac = "00:00:00:00:00:00";
            if (!ipEq.contains("/")) ipEq = ipEq + "/" + redSel.getSlash();

            if (equipoAEditar == null) {
                redSel.agregarEquipo(new Equipo(nombre, desc, ipEq, mac, redSel.getVlanRed(), tipo, estado, usuario, clave));
            } else {
                equipoAEditar.setNombre(nombre); equipoAEditar.setDescripcion(desc);
                equipoAEditar.setIp(ipEq); equipoAEditar.setMac(mac);
                equipoAEditar.setTipo(tipo); equipoAEditar.setEstado(estado);
                equipoAEditar.setUsuario(usuario); equipoAEditar.setContrasena(clave);
            }
            guardarDatos();
            mostrarVistaRedes();
        });

        JButton btnCancelar = crearBotonAccion("Cancelar", new Color(0x2A4A66), () -> mostrarVistaRedes());
        JPanel botones = new JPanel(new GridLayout(1, 2, 12, 0));
        botones.setBackground(BG_CARD); botones.add(btnGuardar); botones.add(btnCancelar);
        botones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        form.add(botones);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 0.85; gbc.weighty = 1.0;
        center.add(form, gbc);

        wrapper.add(center, BorderLayout.CENTER);
        contentPanel.add(wrapper, BorderLayout.CENTER);
        contentPanel.revalidate(); contentPanel.repaint();
    }

    private void mostrarVistaRedes() {
        contentPanel.removeAll();
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

        JPanel cabecera = new JPanel(new BorderLayout(10, 0));
        cabecera.setBackground(BG_DARK);
        cabecera.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JLabel titulo = new JLabel("Topologia de Red");
        titulo.setFont(new Font("Consolas", Font.BOLD, 18));
        titulo.setForeground(TEXT_WHITE);
        cabecera.add(titulo, BorderLayout.WEST);

        JPanel herramientas = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        herramientas.setBackground(BG_DARK);

        JTextField buscador = crearCampoTexto();
        buscador.setPreferredSize(new Dimension(200, 30));
        buscador.setText(filtroActual);
        buscador.addActionListener(e -> {
            filtroActual = buscador.getText().trim().toLowerCase();
            mostrarVistaRedes();
        });
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.setBackground(BG_PANEL); btnBuscar.setForeground(TEXT_WHITE);
        btnBuscar.addActionListener(e -> { filtroActual = buscador.getText().trim().toLowerCase(); mostrarVistaRedes(); });

        JButton btnCsv = new JButton("Exportar CSV");
        btnCsv.setBackground(new Color(0x2E7D32)); btnCsv.setForeground(TEXT_WHITE);
        btnCsv.addActionListener(e -> exportarCSV());

        herramientas.add(new JLabel("Filtrar:"));
        herramientas.add(buscador);
        herramientas.add(btnBuscar);
        herramientas.add(btnCsv);
        cabecera.add(herramientas, BorderLayout.EAST);
        wrapper.add(cabecera, BorderLayout.NORTH);

        List<Red> redesFiltradas = listaRedes;
        if (!filtroActual.isEmpty()) {
            redesFiltradas = listaRedes.stream().filter(r -> 
                r.getIpRed().contains(filtroActual) ||
                r.getEquipos().stream().anyMatch(eq -> eq.getNombre().toLowerCase().contains(filtroActual) || eq.getIp().contains(filtroActual))
            ).collect(Collectors.toList());
        }

        if (redesFiltradas.isEmpty()) {
            JLabel empty = new JLabel("No se encontraron resultados en la topologia.", SwingConstants.CENTER);
            empty.setFont(FONT_BODY); empty.setForeground(TEXT_MUTED);
            wrapper.add(empty, BorderLayout.CENTER);
        } else {
            JPanel listaPanel = new JPanel();
            listaPanel.setLayout(new BoxLayout(listaPanel, BoxLayout.Y_AXIS));
            listaPanel.setBackground(BG_DARK);

            for (Red red : redesFiltradas) {
                listaPanel.add(crearTarjetaRed(red));
                listaPanel.add(Box.createVerticalStrut(14));
            }

            JScrollPane scroll = new JScrollPane(listaPanel);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setBackground(BG_DARK);
            scroll.getViewport().setBackground(BG_DARK);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            estilizarScrollbar(scroll);
            wrapper.add(scroll, BorderLayout.CENTER);
        }

        contentPanel.add(wrapper, BorderLayout.CENTER);
        contentPanel.revalidate(); contentPanel.repaint();
    }

    private JPanel crearTarjetaRed(Red red) {
        JPanel tarjeta = new JPanel(new BorderLayout());
        tarjeta.setBackground(BG_CARD);
        tarjeta.setBorder(new LineBorder(BORDER_COLOR, 1));
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9999));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x152840));
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 4, 0, 0, ACCENT_CYAN), BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel redLabel = new JLabel("  " + red.getIpRed() + "/" + red.getSlash() + " [VLAN " + red.getVlanRed() + "]");
        redLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        redLabel.setForeground(ACCENT_CYAN);
        JLabel bcLabel = new JLabel("Broadcast: " + red.getBroadcast());
        bcLabel.setFont(FONT_SMALL); bcLabel.setForeground(TEXT_MUTED);

        JPanel redInfo = new JPanel(new GridLayout(2, 1));
        redInfo.setOpaque(false); redInfo.add(redLabel); redInfo.add(bcLabel);

        JPanel botonera = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        botonera.setOpaque(false);
        
        JButton btnAgregarEq = new JButton("+ Equipo");
        btnAgregarEq.setBackground(GREEN_OK); btnAgregarEq.setForeground(BG_DARK);
        btnAgregarEq.addActionListener(e -> mostrarFormularioEquipo(red, null));
        
        JButton btnEditarRed = new JButton("Editar");
        btnEditarRed.setBackground(BG_PANEL); btnEditarRed.setForeground(TEXT_WHITE);
        btnEditarRed.addActionListener(e -> mostrarFormularioRed(red));

        JButton btnEliminarRed = new JButton("Borrar");
        btnEliminarRed.setBackground(RED_ERROR); btnEliminarRed.setForeground(TEXT_WHITE);
        btnEliminarRed.addActionListener(e -> {
            int op = JOptionPane.showConfirmDialog(this, "¿Eliminar red " + red.getIpRed() + " y todos sus equipos?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (op == JOptionPane.YES_OPTION) { listaRedes.remove(red); guardarDatos(); mostrarVistaRedes(); }
        });

        botonera.add(btnAgregarEq); botonera.add(btnEditarRed); botonera.add(btnEliminarRed);
        header.add(redInfo, BorderLayout.CENTER);
        header.add(botonera, BorderLayout.EAST);
        tarjeta.add(header, BorderLayout.NORTH);

        if (!red.getEquipos().isEmpty()) {
            String[] columnas = { "Nombre", "IP", "MAC", "Tipo", "Estado", "Usuario", "Clave (Decrypted)" };
            Object[][] filas = new Object[red.getEquipos().size()][7];

            for (int i = 0; i < red.getEquipos().size(); i++) {
                Equipo eq = red.getEquipos().get(i);
                filas[i][0] = eq.getNombre(); filas[i][1] = eq.getIp();
                filas[i][2] = eq.getMac(); filas[i][3] = eq.getTipo();
                filas[i][4] = eq.getEstado(); filas[i][5] = eq.getUsuario();
                // Mostramos la clave desencriptada en la tabla (en memoria está plana)
                filas[i][6] = eq.getContrasena();
            }

            JTable tabla = new JTable(filas, columnas) {
                public boolean isCellEditable(int r, int c) { return false; }
            };
            configurarTablaBase(tabla);

            JScrollPane scrollTabla = new JScrollPane(tabla);
            scrollTabla.setBorder(BorderFactory.createEmptyBorder());
            scrollTabla.getViewport().setBackground(BG_CARD);
            scrollTabla.setPreferredSize(new Dimension(0, Math.min(red.getEquipos().size() * 28 + 28, 200)));
            estilizarScrollbar(scrollTabla);
            
            JPanel accionesTabla = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            accionesTabla.setBackground(BG_CARD);
            JButton btnEditNode = new JButton("Editar Equipo Sel.");
            btnEditNode.addActionListener(e -> {
                int fila = tabla.getSelectedRow();
                if(fila != -1) mostrarFormularioEquipo(red, red.getEquipos().get(fila));
                else mostrarError("Selecciona un equipo de la tabla para editarlo.");
            });
            JButton btnDelNode = new JButton("Eliminar Equipo Sel.");
            btnDelNode.addActionListener(e -> {
                int fila = tabla.getSelectedRow();
                if(fila != -1) {
                    red.eliminarEquipo(red.getEquipos().get(fila));
                    guardarDatos(); mostrarVistaRedes();
                } else mostrarError("Selecciona un equipo de la tabla para eliminarlo.");
            });
            accionesTabla.add(btnEditNode); accionesTabla.add(btnDelNode);

            JPanel contTabla = new JPanel(new BorderLayout());
            contTabla.add(scrollTabla, BorderLayout.CENTER);
            contTabla.add(accionesTabla, BorderLayout.SOUTH);
            tarjeta.add(contTabla, BorderLayout.CENTER);

        } else {
            JLabel vacio = new JLabel("  Sin equipos agregados en esta red.", SwingConstants.LEFT);
            vacio.setFont(FONT_SMALL); vacio.setForeground(TEXT_MUTED);
            vacio.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 0));
            tarjeta.add(vacio, BorderLayout.CENTER);
        }
        return tarjeta;
    }

    private void configurarTablaBase(JTable tabla) {
        tabla.setBackground(BG_CARD);
        tabla.setForeground(TEXT_WHITE);
        tabla.setFont(FONT_MONO);
        tabla.setRowHeight(28);
        tabla.setGridColor(new Color(0x223348));
        tabla.setSelectionBackground(new Color(0x1C4A6E));
        
        JTableHeader th = tabla.getTableHeader();
        th.setBackground(new Color(0x0F1F30)); th.setForeground(TEXT_MUTED);
        th.setFont(FONT_LABEL); th.setReorderingAllowed(false);

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? new Color(0x1C4A6E) : (row % 2 == 0 ? BG_CARD : ROW_ALT));
                if (col == 4) { 
                    String st = val.toString();
                    if(st.equals("Activo")) setForeground(GREEN_OK);
                    else if(st.equals("Inactivo")) setForeground(RED_ERROR);
                    else setForeground(new Color(0xF4A636));
                } else if (col == 6) { setForeground(new Color(0xFF8888)); } 
                else { setForeground(TEXT_WHITE); }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
                return this;
            }
        });
    }

    private void exportarCSV() {
        if(listaRedes.isEmpty()) { mostrarError("No hay datos para exportar."); return; }
        try (PrintWriter pw = new PrintWriter(new File("export_network.csv"))) {
            pw.println("Red,VLAN,Hostname,IP,MAC,Infra,Estado,Usuario");
            for(Red r : listaRedes) {
                for(Equipo e : r.getEquipos()) {
                    pw.println(r.getIpRed()+"/"+r.getSlash() + "," + r.getVlanRed() + "," +
                               e.getNombre() + "," + e.getIp() + "," + e.getMac() + "," + 
                               e.getTipo() + "," + e.getEstado() + "," + e.getUsuario());
                }
            }
            JOptionPane.showMessageDialog(this, "Exportado exitosamente a export_network.csv");
        } catch (Exception ex) {
            mostrarError("Error al exportar: " + ex.getMessage());
        }
    }

    private String calcularBroadcast(String ipRed, int slash) {
        try {
            String[] octetos = ipRed.split("\\.");
            if (octetos.length != 4) return null;
            int[] ip = new int[4];
            for (int i = 0; i < 4; i++) {
                ip[i] = Integer.parseInt(octetos[i]);
                if (ip[i] < 0 || ip[i] > 255) return null;
            }
            int mascara = slash > 0 ? (0xFFFFFFFF << (32 - slash)) : 0;
            int ipInt = (ip[0] << 24) | (ip[1] << 16) | (ip[2] << 8) | ip[3];
            int bcInt = ipInt | (~mascara);
            return ((bcInt >> 24) & 0xFF) + "." + ((bcInt >> 16) & 0xFF) + "."
                 + ((bcInt >>  8) & 0xFF) + "." + (bcInt & 0xFF);
        } catch (Exception e) { return null; }
    }

    // ============================================================
    // METODOS DE CIFRADO AES
    // ============================================================
    private String cifrar(String textoPlano) {
        if (textoPlano == null || textoPlano.isEmpty()) return "";
        try {
            Key key = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            System.err.println("Error al cifrar: " + e.getMessage());
            return textoPlano; 
        }
    }

    private String descifrar(String textoCifrado) {
        if (textoCifrado == null || textoCifrado.isEmpty()) return "";
        try {
            Key key = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decoded = Base64.getDecoder().decode(textoCifrado);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return textoCifrado;
        } catch (Exception e) {
            System.err.println("Error al descifrar (posible texto plano viejo): " + e.getMessage());
            return textoCifrado; 
        }
    }

    private void guardarDatos() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < listaRedes.size(); i++) {
            Red red = listaRedes.get(i);
            json.append("  {\n");
            json.append("    \"ipRed\": \""    ).append(escaparJson(red.getIpRed()))    .append("\",\n");
            json.append("    \"slash\": "      ).append(red.getSlash())                 .append(",\n");
            json.append("    \"vlanRed\": "    ).append(red.getVlanRed())               .append(",\n");
            json.append("    \"broadcast\": \"").append(escaparJson(red.getBroadcast())).append("\",\n");
            json.append("    \"equipos\": [\n");

            List<Equipo> equipos = red.getEquipos();
            for (int j = 0; j < equipos.size(); j++) {
                Equipo eq = equipos.get(j);
                
                String claveCifrada = cifrar(eq.getContrasena());

                json.append("      {\n");
                json.append("        \"nombre\": \""     ).append(escaparJson(eq.getNombre()))     .append("\",\n");
                json.append("        \"descripcion\": \"").append(escaparJson(eq.getDescripcion())).append("\",\n");
                json.append("        \"ip\": \""         ).append(escaparJson(eq.getIp()))         .append("\",\n");
                json.append("        \"mac\": \""        ).append(escaparJson(eq.getMac()))        .append("\",\n");
                json.append("        \"tipo\": \""       ).append(escaparJson(eq.getTipo()))       .append("\",\n");
                json.append("        \"estado\": \""     ).append(escaparJson(eq.getEstado()))     .append("\",\n");
                json.append("        \"usuario\": \""    ).append(escaparJson(eq.getUsuario()))    .append("\",\n");
                json.append("        \"contrasena\": \"" ).append(escaparJson(claveCifrada))       .append("\"\n");
                json.append("      }");
                if (j < equipos.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]\n");
            json.append("  }");
            if (i < listaRedes.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("]");
        try { Files.write(Paths.get(ARCHIVO_DATOS), json.toString().getBytes(StandardCharsets.UTF_8)); } 
        catch (IOException e) { mostrarError("Error guardando JSON: " + e.getMessage()); }
    }

    private String escaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private void cargarDatos() {
        File archivo = new File(ARCHIVO_DATOS);
        if (!archivo.exists()) return; 

        try {
            String contenido = new String(Files.readAllBytes(archivo.toPath()), StandardCharsets.UTF_8);
            listaRedes.clear(); 
            int cursor = 0;
            while (true) {
                int ipPos = contenido.indexOf("\"ipRed\"", cursor);
                if (ipPos == -1) break; 

                String ipRed     = extraerValorJson(contenido, "ipRed", ipPos);
                String slashStr  = extraerValorJson(contenido, "slash", ipPos);
                String broadcast = extraerValorJson(contenido, "broadcast", ipPos);
                String vlanStr   = extraerValorJson(contenido, "vlanRed", ipPos);

                int sl = 24, vlan = 1; 
                try { sl = Integer.parseInt(slashStr.trim()); } catch (Exception ignored) {}
                try { if(!vlanStr.isEmpty()) vlan = Integer.parseInt(vlanStr.trim()); } catch (Exception ignored) {}

                Red red = new Red(ipRed, sl, broadcast, vlan);
                int equiposIni = contenido.indexOf("\"equipos\"", ipPos);
                int equiposFin = encontrarCierreArray(contenido, equiposIni);

                if (equiposIni != -1 && equiposFin != -1) {
                    String bloqueEquipos = contenido.substring(equiposIni, equiposFin);
                    int eqCursor = 0;

                    while (true) {
                        int nombrePos = bloqueEquipos.indexOf("\"nombre\"", eqCursor);
                        if (nombrePos == -1) break;

                        String nombre = extraerValorJson(bloqueEquipos, "nombre", nombrePos);
                        String desc   = extraerValorJson(bloqueEquipos, "descripcion", nombrePos);
                        String ip     = extraerValorJson(bloqueEquipos, "ip", nombrePos);
                        String mac    = extraerValorJson(bloqueEquipos, "mac", nombrePos);
                        String tipo   = extraerValorJson(bloqueEquipos, "tipo", nombrePos);
                        String estado = extraerValorJson(bloqueEquipos, "estado", nombrePos);
                        String user   = extraerValorJson(bloqueEquipos, "usuario", nombrePos);
                        
                        String passLeida = extraerValorJson(bloqueEquipos, "contrasena", nombrePos);
                        String passPlana = descifrar(passLeida);

                        if(mac.isEmpty()) mac = "00:00:00:00:00:00";
                        if(tipo.isEmpty()) tipo = "Físico";
                        if(estado.isEmpty()) estado = "Activo";

                        red.agregarEquipo(new Equipo(nombre, desc, ip, mac, vlan, tipo, estado, user, passPlana));
                        eqCursor = nombrePos + 1; 
                    }
                }
                listaRedes.add(red);
                cursor = ipPos + 1; 
            }
        } catch (Exception e) { mostrarError("Error leyendo datos: " + e.getMessage()); }
    }

    private String extraerValorJson(String json, String clave, int desdePos) {
        int clavePos = json.indexOf("\"" + clave + "\"", desdePos);
        if (clavePos == -1) return "";
        int dosP = json.indexOf(":", clavePos);
        if (dosP == -1) return "";
        int inicio = dosP + 1;
        while (inicio < json.length() && Character.isWhitespace(json.charAt(inicio))) inicio++;
        if (inicio >= json.length()) return "";

        if (json.charAt(inicio) == '"') {
            int fin = inicio + 1;
            while (fin < json.length()) {
                char c = json.charAt(fin);
                if (c == '\\') { fin += 2; continue; } 
                if (c == '"')  break;
                fin++;
            }
            return json.substring(inicio + 1, fin).replace("\\\"", "\"").replace("\\\\", "\\");
        } else {
            int fin = inicio;
            while (fin < json.length()) {
                char c = json.charAt(fin);
                if (c == ',' || c == '}' || c == ']' || c == '\n') break;
                fin++;
            }
            return json.substring(inicio, fin).trim();
        }
    }

    private int encontrarCierreArray(String json, int desdePos) {
        int abierto = json.indexOf("[", desdePos);
        if (abierto == -1) return -1;
        int profundidad = 1, i = abierto + 1;
        while (i < json.length() && profundidad > 0) {
            char c = json.charAt(i);
            if (c == '[') profundidad++;
            if (c == ']') profundidad--;
            i++;
        }
        return i; 
    }

    private JLabel crearLabelForm(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(FONT_LABEL); lbl.setForeground(TEXT_LABEL);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return lbl;
    }

    private JTextField crearCampoTexto() {
        JTextField campo = new JTextField();
        campo.setBackground(BG_INPUT); campo.setForeground(TEXT_WHITE);
        campo.setCaretColor(ACCENT_CYAN); campo.setFont(FONT_MONO);
        campo.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR, 1), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        campo.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { campo.setBorder(BorderFactory.createCompoundBorder(new LineBorder(ACCENT_CYAN, 1), BorderFactory.createEmptyBorder(6, 10, 6, 10))); }
            public void focusLost(FocusEvent e) { campo.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR, 1), BorderFactory.createEmptyBorder(6, 10, 6, 10))); }
        });
        return campo;
    }

    private void estilizarCampo(JTextField campo) {
        campo.setBackground(BG_INPUT); campo.setForeground(TEXT_WHITE);
        campo.setCaretColor(ACCENT_CYAN); campo.setFont(FONT_MONO);
        campo.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR, 1), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
    }

    private <T> void estilizarCombo(JComboBox<T> combo) {
        combo.setBackground(BG_INPUT); combo.setForeground(TEXT_WHITE);
        combo.setFont(FONT_MONO); combo.setBorder(new LineBorder(BORDER_COLOR, 1));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        combo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? BG_CARD : BG_INPUT); setForeground(TEXT_WHITE);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8)); return this;
            }
        });
    }

    private void estilizarScrollbar(JScrollPane sp) {
        sp.getVerticalScrollBar().setBackground(BG_DARK);
        sp.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors() { thumbColor = new Color(0x2A4A66); trackColor = BG_DARK; }
            protected JButton createDecreaseButton(int o) { return botonInvisible(); }
            protected JButton createIncreaseButton(int o) { return botonInvisible(); }
            private JButton botonInvisible() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }
    private void mostrarError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("OptionPane.background", BG_CARD);
        UIManager.put("Panel.background", BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_WHITE);
        UIManager.put("Button.background", new Color(0x2A4A66));
        UIManager.put("Button.foreground", TEXT_WHITE);
        UIManager.put("Button.font", new Font("Consolas", Font.BOLD, 12));
        SwingUtilities.invokeLater(() -> { new NetworkDoc().setVisible(true); });
    }
}