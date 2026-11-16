import { useEffect, useState } from "react";
import { stationsApi } from "../api/endpoints";

/** Liste (id, nom) des stations pour alimenter les filtres déroulants. */
export default function useStationOptions() {
  const [options, setOptions] = useState([]);
  useEffect(() => {
    stationsApi
      .search({ page: 0, size: 100 })
      .then((res) => setOptions(res.content.map((s) => ({ id: s.id, nom: `${s.nom} (${s.ville})` }))))
      .catch(() => setOptions([]));
  }, []);
  return options;
}
