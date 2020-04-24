import logging
from configparser import ConfigParser
import os
from pathlib import Path

path = Path(__file__).parent.parent.parent

# Setting headers to use access_token for the GitHub API
config_fallbacks = {
    'github_access_token': '',
    'endpoint': '',
    'user': '',
    'password': '',
    'server_name': '',
    'prefix': '',
    'graph_base': '',
    'firebase_key': '',
    'local_sparql_dir': '',
    'query_dir': '',
    'context_dir': '',
}
config = ConfigParser(config_fallbacks)
config.add_section('auth')
config.add_section('defaults')
config.add_section('local')
config.read('config.ini')
config_filename = os.path.join(os.path.dirname(os.path.realpath(__file__)), 'config.ini')
config.read(config_filename)

# Default endpoint, if none specified elsewhere
ENDPOINT = config.get('defaults', 'endpoint')
ENDPOINT_USERNAME = config.get('defaults', 'user')
ENDPOINT_PASSWORD = config.get('defaults', 'password')
ENDPOINT_RESOURCE_PREFIX = config.get('defaults', 'prefix')
ENDPOINT_GRAPH_BASE = config.get('defaults', 'graph_base')
FIREBASE_KEY = config.get('defaults', 'firebase_key')

QUERY_DIRECTORY = path/config.get('defaults', 'queries_dir')
CONTEXT_DIRECTORY = path/config.get('defaults', 'context_dir')

mime_types = {
    'csv': 'text/csv; q=1.0, */*; q=0.1',
    'json': 'application/json; q=1.0, application/sparql-results+json; q=0.8, */*; q=0.1',
    'html': 'text/html; q=1.0, */*; q=0.1',
    'ttl': 'text/turtle'
}

UPDATE_ENDPOINT = f'{ENDPOINT}/update'
QUERY_ENDPOINT = f'{ENDPOINT}/query'

QUERIES_TYPES = ["get_all", "get_all_related", "get_all_related_user", "get_all_user", "get_one", "get_one_user"]

logging_file = Path(__file__).parent / "logging.ini"
