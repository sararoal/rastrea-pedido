document.addEventListener("DOMContentLoaded", async () => {
  const btn = document.getElementById("btnBuscar");
  const carrierSelect = document.getElementById("carrier");

  // Cargar carriers desde el JSON de 17track
    try {
    const response = await fetch("apicarrier.all.json");
    const data = await response.json();

    // Ordenar alfabéticamente por nombre
    data.sort((a, b) => {
      if ((a._name || "").toLowerCase() < (b._name || "").toLowerCase()) return -1;
      if ((a._name || "").toLowerCase() > (b._name || "").toLowerCase()) return 1;
      return 0;
    });

        // Guardamos los carriers para filtrar después
    let carriersData = data;

    function renderCarriers(filter = "") {
      carrierSelect.innerHTML = "";
      carriersData
        .filter(carrier => (carrier._name || "").toLowerCase().includes(filter.toLowerCase()))
        .forEach(carrier => {
          const option = document.createElement("option");
          option.value = carrier.key;
          option.textContent = carrier._name;
          carrierSelect.appendChild(option);
        });
    }

    // Mostrar todos al principio
    renderCarriers();

    // Añadir listener al input de búsqueda
    const carrierSearch = document.getElementById("carrierSearch");
    carrierSearch.addEventListener("input", (e) => {
      renderCarriers(e.target.value);
    });

  } catch (err) {
    console.error("No se pudieron cargar los carriers", err);
    const option = document.createElement("option");
    option.value = "";
    option.textContent = "No se pudieron cargar los carriers";
    carrierSelect.appendChild(option);
  }

  // Listener del botón de búsqueda
  btn.addEventListener("click", async () => {
    const numero = document.getElementById("trackingNumber").value;
    const carrier = carrierSelect.value;
    const resultado = document.getElementById("resultado");

    if (!numero) {
      alert("Introduce un número de seguimiento.");
      return;
    }

    if (!carrier) {
      alert("Selecciona la compañía de transporte.");
      return;
    }

    resultado.innerHTML = "<p>Cargando información...</p>";

    try {
      const response = await fetch(`/track/${numero}?carrier=${encodeURIComponent(carrier)}`);
      if (!response.ok) throw new Error("Error en la API");

      const data = await response.json();

      let html = `
        <h2>Seguimiento: ${data.trackingNumber}</h2>
        <p><strong>Transportista:</strong> ${data.carrier}</p>
        <h3>Eventos:</h3>
      `;

      if (data.events && data.events.length > 0) {
        data.events.forEach(evento => {
          html += `
            <div class="evento">
              <strong>${evento.status}</strong>
              <p>📍 ${evento.location || "Ubicación no disponible"}</p>
              <p>📅 ${evento.date}</p>
            </div>
          `;
        });
      } else {
        html += "<p>No hay eventos disponibles.</p>";
      }

      resultado.innerHTML = html;

    } catch (error) {
      resultado.innerHTML = "<p style='color:red;'>Error al obtener el seguimiento.</p>";
    }
  });

  // Inicializa Select2 después de renderizar las opciones
  $(document).ready(function() {
    $('#carrier').select2({
      width: 'resolve',
      placeholder: "Buscar compañía...",
      allowClear: true
    });
  });
});
