import json
from typing import Dict
import uuid
import validators
from rdflib import Graph

from openapi_server import query_manager
from openapi_server.settings import ENDPOINT, PREFIX, GRAPH_BASE, UPDATE_ENDPOINT
from openapi_server import logger


def generate_graph(username):
    return "{}{}".format(GRAPH_BASE, username)


def get_resource(**kwargs):
    """
    This method handles a GET METHOD
    :param kwargs:
    :type kwargs:
    :return:
    :rtype:
    """
    if "id" in kwargs:
        return get_one_resource(**kwargs)
    else:
        return get_all_resource(**kwargs)


def get_one_resource(**kwargs):
    """
    Handles a GET method to get one resource
    :param kwargs:
    :type kwargs:
    :return:
    :rtype:
    """
    username = kwargs["username"]
    owl_class_name = kwargs["rdf_type_name"]
    resource_type_uri = kwargs["rdf_type_uri"]
    query_type = "get_one_user"
    kls = kwargs["kls"]
    request_args: Dict[str, str] = {
        "resource": build_instance_uri(kwargs["id"]),
        "g": generate_graph(username)
    }
    try:
        response = query_manager.obtain_query(owl_class_name=owl_class_name,
                                              owl_class_uri=resource_type_uri,
                                              query_type=query_type,
                                              endpoint=ENDPOINT,
                                              request_args=request_args)
        if response:
            return kls.from_dict(response[0])

    except:
        logger.error("Exception occurred", exc_info=True)
        return "Bad request", 400, {}


def get_all_resource(**kwargs):
    """
    Handles a GET method to get all resource by rdf_type
    :param kwargs:
    :type kwargs:
    :return:
    :rtype:
    """
    resource_type_uri = kwargs["rdf_type_uri"]
    username = kwargs["username"]
    owl_class_name = kwargs["rdf_type_name"]
    kls = kwargs["kls"]
    request_args: Dict[str, str] = {
        "type": resource_type_uri,
        "g": generate_graph(username)
    }

    if "label" in kwargs and kwargs["label"] is not None:
        query_text = kwargs["label"]
        logger.debug("searching by label " + query_text)
        query_type = "get_all_search"
        request_args["text"] = query_text
    else:
        query_type = "get_all_user"

    try:
        response = query_manager.obtain_query(owl_class_name=owl_class_name,
                                              owl_class_uri=resource_type_uri,
                                              query_type=query_type,
                                              endpoint=ENDPOINT,
                                              request_args=request_args)
        items = []
        for d in response:
            items.append(kls.from_dict(d))
        return items
    except:
        logger.error("Exception occurred", exc_info=True)
        return "Bad request", 400, {}


def put_resource(**kwargs):
    resource_uri = build_instance_uri(kwargs["id"])
    body = kwargs["body"]
    body.id = resource_uri

    try:
        username = kwargs["user"]
    except Exception:
        logger.error("Missing username", exc_info=True)
        return "Bad request: missing username", 400, {}

    #DELETE QUERY
    request_args_delete: Dict[str, str] = {
        "resource": resource_uri,
        "g": generate_graph(username)
    }

    try:
        query_manager.delete_query(UPDATE_ENDPOINT, request_args=request_args_delete)
    except:
        return "Error deleting query", 407, {}

    #INSERT QUERY
    body_json = prepare_jsonld(body)
    prefixes, triples = get_insert_query(body_json)
    prefixes = '\n'.join(prefixes)
    triples = '\n'.join(triples)

    request_args: Dict[str, str] = {
        "prefixes": prefixes,
        "triples": triples,
        "g": generate_graph(username)
    }
    if query_manager.insert_query(UPDATE_ENDPOINT, request_args=request_args):
        return body, 201, {}
    else:
        return "Error inserting query", 407, {}


def convert_json_to_triples(body):
    body_json = prepare_jsonld(body)
    prefixes, triples = get_insert_query(body_json)
    prefixes = '\n'.join(prefixes)
    triples = '\n'.join(triples)
    return prefixes, triples


def delete_resource(**kwargs):
    resource_uri = build_instance_uri(kwargs["id"])
    try:
        username = kwargs["user"]
    except Exception:
        logger.error("Missing username", exc_info=True)
        return "Bad request: missing username", 400, {}

    request_args: Dict[str, str] = {
        "resource": resource_uri,
        "g": generate_graph(username)
    }
    return query_manager.delete_query(UPDATE_ENDPOINT, request_args=request_args)


def post_resource(**kwargs):
    """
    Post a resource and generate the id
    :param kwargs:
    :type kwargs:
    :return:
    :rtype:
    """
    body = kwargs["body"]
    rdf_type_uri = kwargs["rdf_type_uri"]
    if body.type and rdf_type_uri is not body.type:
        body.type.append(rdf_type_uri)
    else:
        body.type = [rdf_type_uri]
    body.id = generate_new_uri()
    try:
        username = kwargs["user"]
    except Exception:
        logger.error("Missing username", exc_info=True)
        return "Bad request: missing username", 400, {}

    body_json = prepare_jsonld(body)
    prefixes, triples = get_insert_query(body_json)
    prefixes = '\n'.join(prefixes)
    triples = '\n'.join(triples)

    request_args: Dict[str, str] = {
        "prefixes": prefixes,
        "triples": triples,
        "g": generate_graph(username)
    }
    if query_manager.insert_query(UPDATE_ENDPOINT, request_args=request_args):
        return body, 201, {}
    else:
        return "Error inserting query", 407, {}


def get_insert_query(resource_json):
    prefixes = []
    triples = []
    g = Graph().parse(data=resource_json, format='json-ld', publicID=PREFIX)
    s = g.serialize(format='turtle')
    for n in g.namespace_manager.namespaces():
        prefixes.append(f'PREFIX {n[0]}: <{n[1]}>')

    for line in s.decode().split('\n'):
        if not line.startswith('@prefix'):
            triples.append(line)
    return prefixes, triples


def build_instance_uri(uri):
    if validators.url(uri):
        return uri
    return "{}{}".format(PREFIX, uri)

def generate_new_uri():
    return str(uuid.uuid4())

def prepare_jsonld(resource):
    resource_dict = resource.to_dict()
    resource_dict["id"] = build_instance_uri(resource_dict["id"])
    resource_dict['@context'] = query_manager.context
    resource_json = json.dumps(resource_dict)
    return resource_json
